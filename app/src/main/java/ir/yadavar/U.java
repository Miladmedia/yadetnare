package ir.yadavar;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

final class U {
    static final int PRIMARY = 0xFF3D5AFE, GREEN = 0xFF00C853, RED = 0xFFFF5252, ORANGE = 0xFFFF9100;
    static final String[] MONTHS = {"فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"};

    static SharedPreferences sp(Context c) { return c.getSharedPreferences("y", 0); }
    static boolean fa(Context c) { return !"en".equals(sp(c).getString("lang", "fa")); }
    static String t(Context c, String f, String e) { return fa(c) ? f : e; }

    static boolean dark(Context c) {
        String th = sp(c).getString("theme", "sys");
        if (th.equals("dark")) return true;
        if (th.equals("light")) return false;
        return (Resources.getSystem().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    static Context wrap(Context b) {
        Configuration cf = new Configuration(b.getResources().getConfiguration());
        cf.setLocale(new Locale(fa(b) ? "fa" : "en"));
        cf.uiMode = (cf.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | (dark(b) ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return b.createConfigurationContext(cf);
    }

    static int bg(Context c) { return dark(c) ? 0xFF121218 : 0xFFF4F6FB; }
    static int cardBg(Context c) { return dark(c) ? 0xFF1F2029 : 0xFFFFFFFF; }
    static int tx(Context c) { return dark(c) ? 0xFFF1F1F5 : 0xFF1B1D29; }
    static int sub(Context c) { return dark(c) ? 0xFFA0A3B5 : 0xFF6B6F85; }
    static int field(Context c) { return dark(c) ? 0xFF2A2B36 : 0xFFEDEFF7; }

    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static String n(Context c, long v) {
        String s = Long.toString(v);
        if (!fa(c)) return s;
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) b.append(ch >= '0' && ch <= '9' ? (char) ('۰' + ch - '0') : ch);
        return b.toString();
    }

    static String two(Context c, int v) { return (v < 10 ? n(c, 0) : "") + n(c, v); }
    static String hm(Context c, int min) { return two(c, min / 60) + ":" + two(c, min % 60); }

    static String left(Context c, long d) {
        if (d == 0) return "";
        long ms = d - System.currentTimeMillis();
        if (ms <= 0) return t(c, "⚠ موعد گذشته", "⚠ Overdue");
        long m = ms / 60000;
        long[] v = {m / 43200, m % 43200 / 1440, m % 1440 / 60, m % 60};
        String[] fa = {"ماه", "روز", "ساعت", "دقیقه"}, en = {"mo", "d", "h", "min"};
        List<String> p = new ArrayList<>();
        for (int i = 0; i < 4 && p.size() < 2; i++) if (v[i] > 0) p.add(n(c, v[i]) + " " + t(c, fa[i], en[i]));
        if (p.isEmpty()) return t(c, "کمتر از ۱ دقیقه مانده", "< 1 min left");
        return TextUtilsJoin(p, t(c, " و ", " ")) + t(c, " مانده", " left");
    }

    static String TextUtilsJoin(List<String> p, String sep) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < p.size(); i++) { if (i > 0) b.append(sep); b.append(p.get(i)); }
        return b.toString();
    }

    static android.icu.util.Calendar pcal() {
        return android.icu.util.Calendar.getInstance(new android.icu.util.ULocale("fa_IR@calendar=persian"));
    }

    static String date(Context c, long ms) {
        android.icu.util.Calendar k = pcal();
        k.setTimeInMillis(ms);
        return n(c, k.get(android.icu.util.Calendar.YEAR)) + "/" + two(c, k.get(android.icu.util.Calendar.MONTH) + 1) + "/" + two(c, k.get(android.icu.util.Calendar.DAY_OF_MONTH))
                + " " + two(c, k.get(android.icu.util.Calendar.HOUR_OF_DAY)) + ":" + two(c, k.get(android.icu.util.Calendar.MINUTE));
    }

    interface LongCb { void on(long v); }

    static void pickDate(Activity a, long init, LongCb cb) {
        android.icu.util.Calendar k = pcal();
        k.setTimeInMillis(init == 0 ? System.currentTimeMillis() + 2 * 3600000L : init);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        box.setPadding(dp(a, 16), dp(a, 8), dp(a, 16), 0);
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER);
        NumberPicker y = new NumberPicker(a), mo = new NumberPicker(a), d = new NumberPicker(a);
        int cy = k.get(android.icu.util.Calendar.YEAR);
        y.setMinValue(cy); y.setMaxValue(cy + 10); y.setValue(cy);
        mo.setMinValue(1); mo.setMaxValue(12); mo.setValue(k.get(android.icu.util.Calendar.MONTH) + 1);
        String[] mn = new String[12];
        for (int i = 0; i < 12; i++) mn[i] = MONTHS[i];
        mo.setDisplayedValues(mn);
        d.setMinValue(1); d.setMaxValue(31); d.setValue(k.get(android.icu.util.Calendar.DAY_OF_MONTH));
        row.addView(d); row.addView(mo); row.addView(y);
        TimePicker tp = new TimePicker(a);
        tp.setIs24HourView(true);
        tp.setHour(k.get(android.icu.util.Calendar.HOUR_OF_DAY));
        tp.setMinute(k.get(android.icu.util.Calendar.MINUTE));
        box.addView(row); box.addView(tp);
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        new AlertDialog.Builder(a).setTitle(t(a, "موعد (شمسی)", "Deadline (Jalali)")).setView(sv)
                .setPositiveButton(t(a, "تأیید", "OK"), (dg, w) -> {
                    android.icu.util.Calendar r = pcal();
                    r.clear();
                    r.set(y.getValue(), mo.getValue() - 1, d.getValue(), tp.getHour(), tp.getMinute(), 0);
                    cb.on(r.getTimeInMillis());
                }).setNegativeButton(t(a, "انصراف", "Cancel"), null).show();
    }

    static TextView tv(Context c, String s, float size, int col, boolean bold) {
        TextView v = new TextView(c);
        v.setText(s); v.setTextSize(size); v.setTextColor(col);
        v.setTypeface(c.getResources().getFont(bold ? R.font.vazir_bold : R.font.vazir));
        return v;
    }

    static GradientDrawable rr(int col, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(col); g.setCornerRadius(r);
        return g;
    }

    static Button btn(Context c, String s, int col, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s); b.setAllCaps(false); b.setTextColor(0xFFFFFFFF); b.setTextSize(13);
        b.setTypeface(c.getResources().getFont(R.font.vazir_bold));
        b.setBackground(rr(col, dp(c, 12)));
        b.setMinHeight(0); b.setMinimumHeight(0); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
        b.setOnClickListener(l);
        return b;
    }

    static EditText et(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint); e.setTextColor(tx(c)); e.setHintTextColor(sub(c)); e.setTextSize(15);
        e.setTypeface(c.getResources().getFont(R.font.vazir));
        e.setBackground(rr(field(c), dp(c, 12)));
        e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        return e;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(rr(cardBg(c), dp(c, 20)));
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        return l;
    }

    static LinearLayout.LayoutParams lp(Context c, int w, int h, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = dp(c, top);
        return p;
    }

    static boolean quiet(Context c) {
        SharedPreferences p = sp(c);
        if (!p.getBoolean("q", true)) return false;
        Calendar k = Calendar.getInstance();
        int m = k.get(Calendar.HOUR_OF_DAY) * 60 + k.get(Calendar.MINUTE);
        int s = p.getInt("qs", 1320), e = p.getInt("qe", 420);
        return s < e ? (m >= s && m < e) : (m >= s || m < e);
    }

    static int defInterval(Context c) { return sp(c).getInt("def", 15); }
    static final String[] UF = {"دقیقه", "ساعت", "روز", "هفته", "ماه"}, UE = {"minutes", "hours", "days", "weeks", "months"};
    static final int[] UM = {1, 60, 1440, 10080, 43200};
    static int inputType() { return InputType.TYPE_CLASS_NUMBER; }
}
