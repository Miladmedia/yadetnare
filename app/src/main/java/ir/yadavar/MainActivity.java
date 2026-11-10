package ir.yadavar;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static final String ACTION_QUICK = "ir.yadavar.QUICK";
    Context c;
    LinearLayout list;
    EditText quick;
    Handler h = new Handler(Looper.getMainLooper());
    Runnable refresher = new Runnable() { @Override public void run() { refresh(); h.postDelayed(this, 30000); } };

    @Override protected void attachBaseContext(Context b) { super.attachBaseContext(U.wrap(b)); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        c = this;
        build();
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        handle(getIntent());
    }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); handle(i); }

    void handle(Intent i) {
        if (i == null) return;
        if (Intent.ACTION_SEND.equals(i.getAction())) {
            String t = i.getStringExtra(Intent.EXTRA_TEXT);
            if (t != null) quick.setText(t);
        }
        if (ACTION_QUICK.equals(i.getAction()) || Intent.ACTION_SEND.equals(i.getAction())) {
            quick.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    void build() {
        boolean fa = U.fa(c);
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(U.bg(c));
        root.setLayoutDirection(fa ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setOnApplyWindowInsetsListener((v, in) -> {
            android.graphics.Insets i = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(i.left, i.top, i.right, i.bottom);
            return in;
        });
        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(U.dp(c, 16), U.dp(c, 12), U.dp(c, 16), U.dp(c, 4));
        head.addView(U.tv(c, U.t(c, "یادت نره!", "Don't forget!"), 24, U.PRIMARY, true), new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(U.btn(c, "⚙", U.PRIMARY, v -> startActivity(new Intent(c, SettingsActivity.class))));
        root.addView(head);

        LinearLayout bar = new LinearLayout(c);
        bar.setPadding(U.dp(c, 16), U.dp(c, 8), U.dp(c, 16), U.dp(c, 8));
        quick = U.et(c, U.t(c, "چی یادت نره؟", "What to remember?"));
        quick.setSingleLine(true);
        quick.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        quick.setOnEditorActionListener((v, a, e) -> { quickAdd(); return true; });
        bar.addView(quick, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(U.btn(c, "+", U.PRIMARY, v -> quickAdd()));
        bar.addView(U.btn(c, "…", U.GREEN, v -> form(null)));
        root.addView(bar);

        ScrollView sv = new ScrollView(c);
        list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(U.dp(c, 16), 0, U.dp(c, 16), U.dp(c, 16));
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    void quickAdd() {
        String s = quick.getText().toString().trim();
        if (s.isEmpty()) return;
        Store.add(c, Store.make(c, s, "", U.defInterval(c), 0, 0));
        quick.setText("");
        ReminderService.poke(c);
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this).setMessage(U.t(c, "برای حباب شناور، اجازهٔ نمایش روی برنامه‌های دیگر را بدهید.", "Allow display over other apps for the floating bubble."))
                    .setPositiveButton(U.t(c, "تنظیمات", "Settings"), (d, w) -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))))
                    .setNegativeButton(U.t(c, "بعداً", "Later"), null).show();
        }
        ReminderService.poke(this);
        h.post(refresher);
    }

    @Override protected void onPause() { super.onPause(); h.removeCallbacks(refresher); }

    void refresh() {
        list.removeAllViews();
        List<Reminder> l = Store.all(c);
        if (l.isEmpty()) {
            TextView e = U.tv(c, U.t(c, "کاری باقی نمانده 🎉", "Nothing left 🎉"), 16, U.sub(c), false);
            e.setGravity(Gravity.CENTER);
            list.addView(e, U.lp(c, -1, -2, 40));
        }
        for (Reminder r : l) {
            LinearLayout card = U.card(c);
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(U.tv(c, r.t, 16, U.tx(c), true));
            String lf = U.left(c, r.d);
            if (!lf.isEmpty()) col.addView(U.tv(c, lf, 12, lf.startsWith("⚠") ? U.RED : U.sub(c), false));
            if (!r.n.isEmpty()) col.addView(U.tv(c, r.n, 12, U.sub(c), false));
            row.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(U.btn(c, "✓", U.GREEN, v -> { Store.remove(c, r.id); ReminderService.poke(c); refresh(); }));
            card.addView(row);
            card.setOnClickListener(v -> form(r));
            list.addView(card, U.lp(c, -1, -2, 10));
        }
    }

    long[] dl = {0};
    int[] unit = {0};

    void form(Reminder old) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(U.dp(c, 16), U.dp(c, 8), U.dp(c, 16), 0);
        EditText t = U.et(c, U.t(c, "عنوان", "Title"));
        EditText n = U.et(c, U.t(c, "یادداشت (فقط داخل برنامه)", "Note (in-app only)"));
        EditText iv = U.et(c, "");
        iv.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        int base = old != null ? old.base : U.defInterval(c);
        unit[0] = 0;
        for (int i = U.UM.length - 1; i >= 0; i--) if (base % U.UM[i] == 0) { unit[0] = i; break; }
        iv.setText(Integer.toString(base / U.UM[unit[0]]));
        dl[0] = old != null ? old.d : 0;
        long[] st = {old != null ? old.start : 0};
        if (old != null) { t.setText(old.t); n.setText(old.n); }
        Button ub = U.btn(c, "", U.PRIMARY, null);
        Runnable setU = () -> ub.setText(U.fa(c) ? U.UF[unit[0]] : U.UE[unit[0]]);
        setU.run();
        ub.setOnClickListener(v -> { unit[0] = (unit[0] + 1) % U.UM.length; setU.run(); });
        LinearLayout ir = new LinearLayout(c);
        ir.setGravity(Gravity.CENTER_VERTICAL);
        ir.addView(U.tv(c, U.t(c, "تکرار هر ", "Repeat every "), 14, U.tx(c), false));
        ir.addView(iv, new LinearLayout.LayoutParams(U.dp(c, 70), -2));
        ir.addView(ub);
        Button db = U.btn(c, "", U.ORANGE, null);
        Runnable setD = () -> db.setText(dl[0] == 0 ? U.t(c, "بدون موعد", "No deadline") : U.date(c, dl[0]));
        setD.run();
        db.setOnClickListener(v -> U.pickDate(this, dl[0], x -> { dl[0] = x; setD.run(); }));
        db.setOnLongClickListener(v -> { dl[0] = 0; setD.run(); return true; });
        Button sb = U.btn(c, "", U.GREEN, null);
        Runnable setS = () -> sb.setText(st[0] == 0 ? U.t(c, "شروع: همین حالا", "Start: now") : U.t(c, "شروع: ", "Start: ") + U.date(c, st[0]));
        setS.run();
        sb.setOnClickListener(v -> new TimePickerDialog(this, (p, hh, mm) -> {
            Calendar k = Calendar.getInstance();
            k.set(Calendar.HOUR_OF_DAY, hh); k.set(Calendar.MINUTE, mm); k.set(Calendar.SECOND, 0);
            if (k.getTimeInMillis() < System.currentTimeMillis()) k.add(Calendar.DAY_OF_MONTH, 1);
            st[0] = k.getTimeInMillis(); setS.run();
        }, 9, 0, true).show());
        sb.setOnLongClickListener(v -> { st[0] = 0; setS.run(); return true; });
        box.addView(t); box.addView(n, U.lp(c, -1, -2, 8)); box.addView(ir, U.lp(c, -1, -2, 8));
        box.addView(db, U.lp(c, -1, -2, 8)); box.addView(sb, U.lp(c, -1, -2, 8));
        ScrollView sv = new ScrollView(c);
        sv.addView(box);
        new AlertDialog.Builder(this).setView(sv)
                .setPositiveButton(U.t(c, "ذخیره", "Save"), (d, w) -> {
                    String title = t.getText().toString().trim();
                    if (title.isEmpty()) return;
                    int num = 1;
                    try { num = Math.max(1, Integer.parseInt(iv.getText().toString())); } catch (Exception e) { }
                    int bm = num * U.UM[unit[0]];
                    if (old != null) Store.remove(c, old.id);
                    Reminder r = Store.make(c, title, n.getText().toString().trim(), bm, dl[0], st[0]);
                    if (old != null) r.id = old.id;
                    Store.add(c, r);
                    ReminderService.poke(c);
                    refresh();
                })
                .setNegativeButton(U.t(c, "انصراف", "Cancel"), null).show();
    }
}
