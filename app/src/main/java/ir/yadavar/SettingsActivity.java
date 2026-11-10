package ir.yadavar;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class SettingsActivity extends Activity {
    Context c;
    @Override protected void attachBaseContext(Context b) { super.attachBaseContext(U.wrap(b)); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        c = this;
        build();
    }

    void build() {
        SharedPreferences p = U.sp(c);
        ScrollView sv = new ScrollView(c);
        sv.setBackgroundColor(U.bg(c));
        sv.setLayoutDirection(U.fa(c) ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        sv.setOnApplyWindowInsetsListener((v, in) -> {
            android.graphics.Insets i = in.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(i.left, i.top, i.right, i.bottom);
            return in;
        });
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(U.dp(c, 16), U.dp(c, 16), U.dp(c, 16), U.dp(c, 16));
        sv.addView(box);
        box.addView(U.tv(c, U.t(c, "تنظیمات", "Settings"), 22, U.PRIMARY, true));

        LinearLayout g1 = section(box, U.t(c, "ظاهر و زبان", "Appearance & language"));
        String[] th = {"sys", "light", "dark"};
        String[] thn = {U.t(c, "سیستم", "System"), U.t(c, "روشن", "Light"), U.t(c, "تیره", "Dark")};
        g1.addView(chips(thn, java.util.Arrays.asList(th).indexOf(p.getString("theme", "sys")), i -> { p.edit().putString("theme", th[i]).apply(); recreate(); }));
        g1.addView(chips(new String[]{"فارسی", "English"}, U.fa(c) ? 0 : 1, i -> { p.edit().putString("lang", i == 0 ? "fa" : "en").apply(); recreate(); }), U.lp(c, -1, -2, 8));

        LinearLayout g2 = section(box, U.t(c, "هشدارها", "Alerts"));
        g2.addView(U.tv(c, U.t(c, "فاصلهٔ پیش‌فرض تکرار (دقیقه)", "Default repeat (minutes)"), 14, U.tx(c), false));
        EditText def = num(p.getInt("def", 15));
        g2.addView(def);
        g2.addView(U.tv(c, U.t(c, "حداکثر هشدار در ساعت", "Max alerts per hour"), 14, U.tx(c), false), U.lp(c, -2, -2, 8));
        EditText mx = num(p.getInt("maxh", 6));
        g2.addView(mx);
        Switch bub = sw(U.t(c, "نمایش حباب شناور", "Floating bubble"), p.getBoolean("bubble", true));
        g2.addView(bub, U.lp(c, -1, -2, 8));

        LinearLayout g3 = section(box, U.t(c, "حالت خواب (اعلان بی‌صدا)", "Sleep mode (silent notifications)"));
        Switch q = sw(U.t(c, "فعال", "Enabled"), p.getBoolean("q", true));
        g3.addView(q);
        int[] qs = {p.getInt("qs", 1320)}, qe = {p.getInt("qe", 420)};
        Button b1 = U.btn(c, "", U.PRIMARY, null), b2 = U.btn(c, "", U.PRIMARY, null);
        Runnable up = () -> { b1.setText(U.t(c, "از ", "From ") + U.hm(c, qs[0])); b2.setText(U.t(c, "تا ", "To ") + U.hm(c, qe[0])); };
        up.run();
        b1.setOnClickListener(v -> new TimePickerDialog(c, (t, h, m) -> { qs[0] = h * 60 + m; up.run(); }, qs[0] / 60, qs[0] % 60, true).show());
        b2.setOnClickListener(v -> new TimePickerDialog(c, (t, h, m) -> { qe[0] = h * 60 + m; up.run(); }, qe[0] / 60, qe[0] % 60, true).show());
        LinearLayout r = new LinearLayout(c);
        r.addView(b1); r.addView(b2, U.lp(c, -2, -2, 0));
        g3.addView(r, U.lp(c, -1, -2, 8));
        g3.addView(U.tv(c, U.t(c, "در این بازه صدا و لرزش قطع می‌شود؛ حباب همچنان نمایش داده می‌شود.", "No sound/vibration in this period; the bubble stays visible."), 12, U.sub(c), false), U.lp(c, -2, -2, 6));

        box.addView(U.btn(c, U.t(c, "معافیت از بهینه‌سازی باتری", "Battery optimization exemption"), U.ORANGE, v ->
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())))), U.lp(c, -1, -2, 12));
        box.addView(U.btn(c, U.t(c, "ذخیره", "Save"), U.GREEN, v -> {
            p.edit().putInt("def", val(def, 15)).putInt("maxh", val(mx, 6)).putBoolean("bubble", bub.isChecked())
                    .putBoolean("q", q.isChecked()).putInt("qs", qs[0]).putInt("qe", qe[0]).apply();
            ReminderService.poke(c);
            finish();
        }), U.lp(c, -1, -2, 8));

        LinearLayout g4 = section(box, U.t(c, "درباره من", "About me"));
        TextView a = U.tv(c, "میلاد مدیا\nhttps://ble.ir/miladmedia_ir", 15, U.PRIMARY, true);
        a.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://ble.ir/miladmedia_ir"))));
        g4.addView(a);
        setContentView(sv);
    }

    interface IntCb { void on(int i); }

    LinearLayout section(LinearLayout parent, String title) {
        LinearLayout l = U.card(c);
        l.addView(U.tv(c, title, 15, U.tx(c), true));
        parent.addView(l, U.lp(c, -1, -2, 12));
        return l;
    }

    LinearLayout chips(String[] names, int sel, IntCb cb) {
        LinearLayout r = new LinearLayout(c);
        for (int i = 0; i < names.length; i++) {
            final int k = i;
            r.addView(U.btn(c, names[i], i == sel ? U.PRIMARY : U.sub(c), v -> cb.on(k)), U.lp(c, -2, -2, 8));
        }
        return r;
    }

    EditText num(int v) {
        EditText e = U.et(c, "");
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        e.setText(Integer.toString(v));
        return e;
    }

    int val(EditText e, int d) { try { return Math.max(1, Integer.parseInt(e.getText().toString())); } catch (Exception x) { return d; } }

    Switch sw(String t, boolean on) {
        Switch s = new Switch(c);
        s.setText(t); s.setChecked(on); s.setTextColor(U.tx(c));
        return s;
    }
}
