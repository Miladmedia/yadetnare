package ir.yadavar;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.List;

class Bubble {
    final Context svc, c;
    final WindowManager wm;
    TextView b;
    FrameLayout panel;
    WindowManager.LayoutParams bp;
    final int size;

    Bubble(Context svc, Context c) {
        this.svc = svc; this.c = c;
        wm = svc.getSystemService(WindowManager.class);
        size = U.dp(c, 52);
    }

    WindowManager.LayoutParams lp(int w, int h, int flags) {
        return new WindowManager.LayoutParams(w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT);
    }

    void update(List<Reminder> l, boolean urgent) {
        if (b == null) create();
        b.setText(U.n(c, l.size()));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, urgent ? new int[]{0xFFFF5252, 0xFFFF9100} : new int[]{0xFF7C4DFF, 0xFF2979FF, 0xFF00E5FF});
        g.setShape(GradientDrawable.OVAL);
        b.setBackground(g);
        if (panel != null) fill(l);
    }

    void create() {
        b = U.tv(c, "", 18, 0xFFFFFFFF, true);
        b.setGravity(Gravity.CENTER);
        b.setElevation(U.dp(c, 6));
        bp = lp(size, size, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        bp.gravity = Gravity.TOP | Gravity.LEFT;
        DisplayMetrics_fix();
        bp.x = U.sp(svc).getInt("bx", sw() - size);
        bp.y = U.sp(svc).getInt("by", sh() / 3);
        b.setOnTouchListener(new View.OnTouchListener() {
            float dx, dy; int sx, sy; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN: dx = e.getRawX(); dy = e.getRawY(); sx = bp.x; sy = bp.y; moved = false; return true;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(e.getRawX() - dx) + Math.abs(e.getRawY() - dy) > U.dp(c, 8)) moved = true;
                        if (moved) {
                            bp.x = clampX(sx + (int) (e.getRawX() - dx)); bp.y = clampY(sy + (int) (e.getRawY() - dy));
                            wm.updateViewLayout(b, bp);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) openPanel();
                        else {
                            bp.x = bp.x + size / 2 < sw() / 2 ? 0 : sw() - size;
                            wm.updateViewLayout(b, bp);
                            U.sp(svc).edit().putInt("bx", bp.x).putInt("by", bp.y).apply();
                        }
                        return true;
                }
                return false;
            }
        });
        wm.addView(b, bp);
    }

    void DisplayMetrics_fix() { }
    int sw() { return wm.getCurrentWindowMetrics().getBounds().width(); }
    int sh() { return wm.getCurrentWindowMetrics().getBounds().height(); }
    int clampX(int x) { return Math.max(0, Math.min(sw() - size, x)); }
    int clampY(int y) {
        android.graphics.Insets in = wm.getCurrentWindowMetrics().getWindowInsets().getInsets(WindowInsets.Type.systemBars());
        return Math.max(in.top, Math.min(sh() - in.bottom - size, y));
    }

    LinearLayout list;

    void openPanel() {
        if (panel != null) return;
        panel = new FrameLayout(c);
        panel.setBackgroundColor(0x88000000);
        panel.setLayoutDirection(U.fa(c) ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        panel.setOnClickListener(v -> closePanel());
        LinearLayout card = U.card(c);
        card.setOnClickListener(v -> { });
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        fp.setMargins(U.dp(c, 20), 0, U.dp(c, 20), 0);
        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = U.tv(c, U.t(c, "کارهای باقی‌مانده", "Pending"), 16, U.tx(c), true);
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        TextView x = U.tv(c, "✕", 20, U.sub(c), true);
        x.setPadding(U.dp(c, 8), 0, U.dp(c, 8), 0);
        x.setOnClickListener(v -> closePanel());
        head.addView(x);
        card.addView(head);
        ScrollView sv = new ScrollView(c);
        list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        card.addView(sv, new LinearLayout.LayoutParams(-1, -2));
        EditText in = U.et(c, U.t(c, "افزودن کار جدید…", "Add new task…"));
        in.setSingleLine(true);
        Button add = U.btn(c, "+", U.PRIMARY, v -> {
            String s = in.getText().toString().trim();
            if (s.isEmpty()) return;
            Store.add(svc, Store.make(svc, s, "", U.defInterval(svc), 0, 0));
            in.setText("");
            ReminderService.poke(svc);
        });
        LinearLayout row = new LinearLayout(c);
        row.addView(in, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(add);
        card.addView(row, U.lp(c, -1, -2, 10));
        panel.addView(card, fp);
        WindowManager.LayoutParams p = lp(-1, -1, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        panel.setAlpha(0);
        wm.addView(panel, p);
        panel.animate().alpha(1).setDuration(150).start();
        b.animate().alpha(0.3f).setDuration(150).start();
        fill(Store.all(svc));
    }

    void fill(List<Reminder> l) {
        if (list == null) return;
        list.removeAllViews();
        if (l.isEmpty()) closePanel();
        for (Reminder r : l) {
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(U.tv(c, r.t, 15, U.tx(c), true));
            String lf = U.left(c, r.d);
            if (!lf.isEmpty()) col.addView(U.tv(c, lf, 12, lf.startsWith("⚠") ? U.RED : U.sub(c), false));
            row.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(U.btn(c, "💤", U.ORANGE, v -> { Store.snooze(svc, r.id, System.currentTimeMillis() + 30 * 60000L); ReminderService.poke(svc); }));
            row.addView(U.btn(c, "✓", U.GREEN, v -> { Store.remove(svc, r.id); ReminderService.poke(svc); }), U.lp(c, -2, -2, 0));
            list.addView(row, U.lp(c, -1, -2, 8));
        }
    }

    void closePanel() {
        if (panel == null) return;
        View p = panel;
        panel = null; list = null;
        try { wm.removeView(p); } catch (Exception e) { }
        if (b != null) b.animate().alpha(1).setDuration(150).start();
    }

    void hide() {
        closePanel();
        if (b != null) { try { wm.removeView(b); } catch (Exception e) { } b = null; }
    }
}
