package ir.yadavar;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import java.util.List;

class Bubble {
    final Context svc, c;
    final WindowManager wm;
    TextView b;
    FrameLayout panel;
    View panelCard;
    boolean panelClosing;
    WindowManager.LayoutParams bp;
    final int size;
    GlassBubbleDrawable bubbleBg;

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
        if (bubbleBg == null) {
            bubbleBg = new GlassBubbleDrawable();
            b.setBackground(bubbleBg);
        }
        bubbleBg.setMood(l.size(), urgent);
        if (panel != null && !panelClosing) fill(l);
    }

    void create() {
        b = U.tv(c, "", 18, 0xFFFFFFFF, true);
        b.setGravity(Gravity.CENTER);
        b.setIncludeFontPadding(false);
        b.setShadowLayer(U.dp(c, 2), 0, U.dp(c, 1), 0x77000000);
        b.setElevation(U.dp(c, 8));
        bubbleBg = new GlassBubbleDrawable();
        b.setBackground(bubbleBg);
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
                        if (!moved) {
                            if (panel != null) closePanel();
                            else openPanel();
                        } else {
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
        if (panel != null || panelClosing) return;
        panel = new FrameLayout(c);
        panel.setBackgroundColor(0x88000000);
        panel.setLayoutDirection(U.fa(c) ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        panel.setOnClickListener(v -> closePanel());
        LinearLayout card = U.card(c);
        panelCard = card;
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

        panel.setAlpha(0f);
        card.setAlpha(0f);
        card.setScaleX(0.92f);
        card.setScaleY(0.92f);
        card.setTranslationY(U.dp(c, 24));

        wm.addView(panel, p);
        fill(Store.all(svc));

        panel.animate().alpha(1f).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setDuration(220).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        b.animate().alpha(0.3f).setDuration(150).start();
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
        if (panel == null || panelClosing) return;
        panelClosing = true;
        final View p = panel;
        final View card = panelCard;
        p.setOnClickListener(null);
        if (card != null) {
            card.animate().cancel();
            card.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f).translationY(U.dp(c, 18)).setDuration(150)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator()).start();
        }
        p.animate().cancel();
        p.animate().alpha(0f).setDuration(170).setInterpolator(new android.view.animation.AccelerateInterpolator()).withEndAction(() -> {
            try { wm.removeView(p); } catch (Exception e) { }
            if (panel == p) {
                panel = null;
                panelCard = null;
                list = null;
            }
            panelClosing = false;
        }).start();
        if (b != null) b.animate().alpha(1f).setDuration(150).start();
    }

    void hide() {
        if (panel != null) {
            try {
                panel.animate().cancel();
                if (panelCard != null) panelCard.animate().cancel();
                wm.removeView(panel);
            } catch (Exception e) { }
            panel = null;
            panelCard = null;
            list = null;
            panelClosing = false;
        }
        if (bubbleBg != null) bubbleBg.stop();
        if (b != null) { try { wm.removeView(b); } catch (Exception e) { } b = null; }
        bubbleBg = null;
    }

    static class GlassBubbleDrawable extends Drawable {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint wave = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF r = new RectF();
        final Path clip = new Path();
        int count;
        boolean urgent;
        boolean running = true;
        long start = SystemClock.uptimeMillis();

        final Runnable tick = new Runnable() {
            @Override public void run() {
                if (!running) return;
                invalidateSelf();
                scheduleSelf(this, SystemClock.uptimeMillis() + 33);
            }
        };

        GlassBubbleDrawable() {
            stroke.setStyle(Paint.Style.STROKE);
            wave.setStyle(Paint.Style.STROKE);
            wave.setStrokeCap(Paint.Cap.ROUND);
            shine.setStyle(Paint.Style.STROKE);
            shine.setStrokeCap(Paint.Cap.ROUND);
        }

        void setMood(int count, boolean urgent) {
            this.count = Math.max(0, count);
            this.urgent = urgent;
            invalidateSelf();
        }

        void stop() {
            running = false;
            unscheduleSelf(tick);
        }

        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            if (b.width() <= 0 || b.height() <= 0) return;

            float w = b.width();
            float h = b.height();
            float cx = b.left + w / 2f;
            float cy = b.top + h / 2f;
            float t = (SystemClock.uptimeMillis() - start) / 1000f;
            float pulse = urgent ? 1f + 0.045f * (float) Math.sin(t * 5.5f) : 1f + 0.018f * (float) Math.sin(t * 2.2f);

            float inset = Math.max(1f, w * 0.045f);
            r.set(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);

            canvas.save();
            canvas.scale(pulse, pulse, cx, cy);

            float hue = urgent ? 8f + 16f * (float) Math.sin(t * 1.7f) : (202f + count * 19f + t * 16f) % 360f;
            int c1 = urgent ? Color.HSVToColor(225, new float[]{hue, 0.72f, 1.00f}) : Color.HSVToColor(170, new float[]{hue, 0.42f, 1.00f});
            int c2 = urgent ? Color.HSVToColor(210, new float[]{32f, 0.84f, 1.00f}) : Color.HSVToColor(150, new float[]{(hue + 46f) % 360f, 0.58f, 1.00f});
            int c3 = urgent ? Color.HSVToColor(165, new float[]{352f, 0.62f, 1.00f}) : Color.HSVToColor(115, new float[]{(hue + 112f) % 360f, 0.45f, 1.00f});

            float drift = (float) Math.sin(t * 0.9f) * w * 0.18f;
            p.setShader(new LinearGradient(
                    r.left - drift, r.top,
                    r.right + drift, r.bottom,
                    new int[]{c1, c2, c3},
                    new float[]{0f, 0.56f, 1f},
                    Shader.TileMode.CLAMP
            ));
            p.setStyle(Paint.Style.FILL);
            canvas.drawOval(r, p);

            p.setShader(new RadialGradient(
                    r.left + w * (0.30f + 0.06f * (float) Math.sin(t)),
                    r.top + h * 0.22f,
                    w * 0.72f,
                    new int[]{0xBBFFFFFF, 0x33FFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawOval(r, p);

            p.setShader(new RadialGradient(
                    r.right - w * 0.15f,
                    r.bottom - h * 0.10f,
                    w * 0.75f,
                    new int[]{urgent ? 0x33FFB199 : 0x335CEBFF, 0x00000000},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawOval(r, p);

            clip.reset();
            clip.addOval(r, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);

            wave.setShader(null);
            wave.setStrokeWidth(Math.max(1.4f, w * 0.045f));
            for (int i = 0; i < 3; i++) {
                float yy = r.top + h * (0.42f + i * 0.13f) + (float) Math.sin(t * 1.35f + i) * h * 0.035f;
                float off = ((t * (urgent ? 46f : 30f)) + i * w * 0.28f) % (w * 1.25f);
                wave.setColor(i == 0 ? 0x36FFFFFF : 0x24FFFFFF);
                Path path = new Path();
                path.moveTo(r.left - w * 0.35f + off, yy);
                path.cubicTo(r.left + w * 0.05f + off, yy - h * 0.15f, r.left + w * 0.30f + off, yy + h * 0.15f, r.left + w * 0.70f + off, yy);
                path.cubicTo(r.left + w * 0.96f + off, yy - h * 0.10f, r.left + w * 1.15f + off, yy + h * 0.08f, r.left + w * 1.42f + off, yy);
                canvas.drawPath(path, wave);
            }

            canvas.restore();

            shine.setShader(null);
            shine.setStrokeWidth(Math.max(1.5f, w * 0.05f));
            shine.setColor(0xAFFFFFFF);
            RectF topArc = new RectF(r.left + w * 0.18f, r.top + h * 0.12f, r.right - w * 0.20f, r.bottom - h * 0.30f);
            canvas.drawArc(topArc, 204, 82, false, shine);

            shine.setStrokeWidth(Math.max(1f, w * 0.026f));
            shine.setColor(0x66FFFFFF);
            RectF sideArc = new RectF(r.left + w * 0.12f, r.top + h * 0.20f, r.right - w * 0.10f, r.bottom - h * 0.10f);
            canvas.drawArc(sideArc, 320, 54, false, shine);

            stroke.setShader(null);
            stroke.setStrokeWidth(Math.max(1f, w * 0.028f));
            stroke.setColor(urgent ? 0xA6FFFFFF : 0x8CFFFFFF);
            canvas.drawOval(r, stroke);

            stroke.setStrokeWidth(Math.max(1f, w * 0.012f));
            stroke.setColor(urgent ? 0x55FFEBEE : 0x4DFFFFFF);
            RectF inner = new RectF(r.left + w * 0.07f, r.top + h * 0.07f, r.right - w * 0.07f, r.bottom - h * 0.07f);
            canvas.drawOval(inner, stroke);

            canvas.restore();

            if (running && isVisible()) {
                unscheduleSelf(tick);
                scheduleSelf(tick, SystemClock.uptimeMillis() + 33);
            }
        }

        @Override public void setAlpha(int alpha) {
            p.setAlpha(alpha);
            stroke.setAlpha(alpha);
            wave.setAlpha(alpha);
            shine.setAlpha(alpha);
            invalidateSelf();
        }

        @Override public void setColorFilter(ColorFilter colorFilter) {
            p.setColorFilter(colorFilter);
            stroke.setColorFilter(colorFilter);
            wave.setColorFilter(colorFilter);
            shine.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override public boolean setVisible(boolean visible, boolean restart) {
            boolean changed = super.setVisible(visible, restart);
            if (visible) {
                running = true;
                invalidateSelf();
            } else {
                stop();
            }
            return changed;
        }
    }
}