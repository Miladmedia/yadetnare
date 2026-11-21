package ir.yadavar;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class ReminderService extends Service {
    static final String ACT_TICK = "ir.yadavar.TICK", ACT_DONE = "ir.yadavar.DONE", ACT_SNOOZE = "ir.yadavar.SNOOZE";
    static final int FG_ID = 1;
    Handler h = new Handler(Looper.getMainLooper());
    Bubble bubble;
    Context ctx;
    Ringtone ring;
    Set<Long> posted = new HashSet<>();
    List<Long> alertLog = new ArrayList<>();
    long lastAlert;
    View glassAlert;
    Runnable glassDismiss;
    boolean glassClosing;
    Runnable tick = new Runnable() { @Override public void run() { tick(); h.postDelayed(this, 15000); } };

    static void poke(Context c) {
        Intent i = new Intent(c, ReminderService.class).setAction(ACT_TICK);
        try { c.startForegroundService(i); } catch (Exception e) { }
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        ctx = U.wrap(this);
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel st = new NotificationChannel("status", U.t(ctx, "وضعیت", "Status"), NotificationManager.IMPORTANCE_LOW);
        NotificationChannel al = new NotificationChannel("alert", U.t(ctx, "یادآوری", "Alerts"), NotificationManager.IMPORTANCE_HIGH);
        al.enableVibration(true);
        nm.createNotificationChannel(st);
        nm.createNotificationChannel(al);
        h.post(tick);
    }

    @Override public int onStartCommand(Intent in, int f, int id) {
        ctx = U.wrap(this);
        startForeground(FG_ID, base(Store.all(this).size(), null));
        if (in != null && in.getAction() != null) {
            long rid = in.getLongExtra("id", 0);
            if (ACT_DONE.equals(in.getAction())) Store.remove(this, rid);
            else if (ACT_SNOOZE.equals(in.getAction())) Store.snooze(this, rid, System.currentTimeMillis() + 30 * 60000L);
            if (!ACT_TICK.equals(in.getAction())) {
                stopRing();
                hideGlassAlert(true);
            }
        }
        tick();
        return START_STICKY;
    }

    Notification base(int count, String text) {
        Intent open = new Intent(this, MainActivity.class);
        Notification.Builder b = new Notification.Builder(this, "status").setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(U.t(ctx, "یادت نره!", "Don't forget!"))
                .setContentText(text != null ? text : (count == 0 ? U.t(ctx, "کاری باقی نمانده", "Nothing left") : U.n(ctx, count) + U.t(ctx, " کار انجام‌نشده", " pending")))
                .setContentIntent(PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE)).setOngoing(true);
        return b.build();
    }

    PendingIntent act(String a, long id) {
        Intent i = new Intent(this, ReminderService.class).setAction(a).putExtra("id", id);
        return PendingIntent.getForegroundService(this, (int) (id % 100000) * 3 + (a.equals(ACT_DONE) ? 1 : 2), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    void tick() {
        long now = System.currentTimeMillis();
        List<Reminder> l = Store.all(this);
        NotificationManager nm = getSystemService(NotificationManager.class);
        Set<Long> ids = new HashSet<>();
        for (Reminder r : l) ids.add(r.id);
        for (Iterator<Long> i = posted.iterator(); i.hasNext(); ) { long x = i.next(); if (!ids.contains(x)) { nm.cancel((int) (x % 1000000) + 10); i.remove(); } }

        List<Reminder> due = new ArrayList<>();
        for (Reminder r : l) if (r.start <= now && r.next(now) <= now) due.add(r);

        boolean quiet = U.quiet(this);
        boolean alertNow = false;
        if (!due.isEmpty() && !quiet) {
            alertLog.removeIf(t -> now - t > 3600000L);
            int max = U.sp(this).getInt("maxh", 6);
            alertNow = alertLog.size() < max && now - lastAlert >= 120000L;
        }
        if (alertNow) { alertLog.add(now); lastAlert = now; for (Reminder r : due) r.last = now; Store.save(this, l); }

        boolean urgent = false;
        for (Reminder r : l) {
            if (r.start > now) continue;
            boolean isDue = alertNow && due.contains(r);
            if (r.d != 0 && r.d - now < 3600000L) urgent = true;
            String lf = U.left(ctx, r.d);
            Notification.Builder b = new Notification.Builder(this, isDue ? "alert" : "status").setSmallIcon(R.drawable.ic_bell)
                    .setContentTitle(r.t).setContentText(lf.isEmpty() ? r.n : lf).setOngoing(true).setOnlyAlertOnce(!isDue)
                    .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
                    .addAction(new Notification.Action.Builder(null, U.t(ctx, "انجام شد", "Done"), act(ACT_DONE, r.id)).build())
                    .addAction(new Notification.Action.Builder(null, U.t(ctx, "بعداً (۳۰ دقیقه)", "Later (30 min)"), act(ACT_SNOOZE, r.id)).build());
            nm.notify((int) (r.id % 1000000) + 10, b.build());
            posted.add(r.id);
        }
        if (alertNow) {
            startRing();
            showGlassAlert(due);
        }

        int count = 0;
        for (Reminder r : l) if (r.start <= now) count++;
        nm.notify(FG_ID, base(count, null));

        boolean ov = Settings.canDrawOverlays(this) && U.sp(this).getBoolean("bubble", true);
        if (ov && !l.isEmpty()) {
            if (bubble == null) bubble = new Bubble(this, ctx);
            bubble.update(l, urgent);
        } else if (bubble != null) { bubble.hide(); bubble = null; }
        QuickWidget.update(this);

        long next = now + 60000L;
        for (Reminder r : l) next = Math.min(next, Math.max(now + 1000, r.next(now)));
        AlarmManager am = getSystemService(AlarmManager.class);
        PendingIntent pi = PendingIntent.getForegroundService(this, 99, new Intent(this, ReminderService.class).setAction(ACT_TICK), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
        } catch (Exception e) { }
    }

    void showGlassAlert(List<Reminder> due) {
        if (due == null || due.isEmpty()) return;
        if (!U.sp(this).getBoolean("glass_alert", true)) return;
        if (!Settings.canDrawOverlays(this)) return;

        PowerManager pm = getSystemService(PowerManager.class);
        if (pm == null || !pm.isInteractive()) return;

        if (glassDismiss != null) h.removeCallbacks(glassDismiss);
        hideGlassAlert(false);

        WindowManager wm = getSystemService(WindowManager.class);
        if (wm == null) return;

        FrameLayout root = new FrameLayout(ctx);
        root.setLayoutDirection(U.fa(ctx) ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(U.dp(ctx, 16), U.dp(ctx, 36), U.dp(ctx, 16), U.dp(ctx, 16));
        root.setOnClickListener(v -> hideGlassAlert(true));

        final LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(U.dp(ctx, 16), U.dp(ctx, 14), U.dp(ctx, 16), U.dp(ctx, 14));
        card.setElevation(U.dp(ctx, 10));
        card.setOnClickListener(v -> { });

        boolean dark = isDark(U.bg(ctx));
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                dark ? new int[]{0xCC182232, 0xAA26364A} : new int[]{0xDDFFFFFF, 0xBFEAF8FF}
        );
        bg.setCornerRadius(U.dp(ctx, 24));
        bg.setStroke(U.dp(ctx, 1), dark ? 0x55FFFFFF : 0x99FFFFFF);
        card.setBackground(bg);

        LinearLayout head = new LinearLayout(ctx);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = U.tv(ctx, due.size() == 1 ? U.t(ctx, "یادآوری", "Reminder") : U.t(ctx, "یادآوری‌ها", "Reminders"), 15, U.PRIMARY, true);
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = U.tv(ctx, "✕", 20, U.sub(ctx), true);
        close.setGravity(Gravity.CENTER);
        close.setPadding(U.dp(ctx, 10), 0, U.dp(ctx, 10), 0);
        close.setOnClickListener(v -> hideGlassAlert(true));
        head.addView(close);
        card.addView(head);

        if (due.size() == 1) {
            Reminder r = due.get(0);
            card.addView(U.tv(ctx, r.t, 20, U.tx(ctx), true), U.lp(ctx, -1, -2, 8));
            String lf = U.left(ctx, r.d);
            if (!lf.isEmpty()) card.addView(U.tv(ctx, lf, 13, lf.startsWith("⚠") ? U.RED : U.sub(ctx), false), U.lp(ctx, -1, -2, 4));
            if (!r.n.isEmpty()) card.addView(U.tv(ctx, r.n, 13, U.sub(ctx), false), U.lp(ctx, -1, -2, 4));
        } else {
            card.addView(U.tv(ctx, U.n(ctx, due.size()) + U.t(ctx, " یادآوری هم‌زمان", " reminders now"), 18, U.tx(ctx), true), U.lp(ctx, -1, -2, 8));
            LinearLayout rows = new LinearLayout(ctx);
            rows.setOrientation(LinearLayout.VERTICAL);
            int shown = 0;
            for (Reminder r : due) {
                if (shown >= 6) break;
                rows.addView(U.tv(ctx, "• " + r.t, 15, U.tx(ctx), true), U.lp(ctx, -1, -2, 4));
                shown++;
            }
            if (due.size() > shown) {
                rows.addView(U.tv(ctx, U.t(ctx, "و چند مورد دیگر…", "and more…"), 13, U.sub(ctx), false), U.lp(ctx, -1, -2, 4));
            }
            card.addView(rows, U.lp(ctx, -1, -2, 4));
        }

        TextView hint = U.tv(ctx, U.t(ctx, "برای بستن لمس کنید", "Tap to dismiss"), 12, U.sub(ctx), false);
        hint.setGravity(Gravity.CENTER);
        card.addView(hint, U.lp(ctx, -1, -2, 10));

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        cp.setMargins(U.dp(ctx, 8), 0, U.dp(ctx, 8), 0);
        root.addView(card, cp);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                -1, -1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.TOP | Gravity.LEFT;

        root.setAlpha(0f);
        card.setAlpha(0f);
        card.setScaleX(0.94f);
        card.setScaleY(0.94f);
        card.setTranslationY(-U.dp(ctx, 20));

        try {
            wm.addView(root, lp);
            glassAlert = root;
            glassClosing = false;
            root.animate().alpha(1f).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            card.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setDuration(220).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            glassDismiss = () -> hideGlassAlert(true);
            h.postDelayed(glassDismiss, 5000);
        } catch (Exception e) {
            glassAlert = null;
            glassClosing = false;
        }
    }

    void hideGlassAlert(boolean animate) {
        final View v = glassAlert;
        if (v == null) return;
        if (glassDismiss != null) {
            h.removeCallbacks(glassDismiss);
            glassDismiss = null;
        }
        if (glassClosing && animate) return;
        glassClosing = true;

        Runnable remove = () -> {
            try {
                WindowManager wm = getSystemService(WindowManager.class);
                if (wm != null) wm.removeView(v);
            } catch (Exception e) { }
            if (glassAlert == v) glassAlert = null;
            glassClosing = false;
        };

        if (!animate) {
            try { v.animate().cancel(); } catch (Exception e) { }
            remove.run();
            return;
        }

        v.animate().cancel();
        v.animate().alpha(0f).setDuration(180).setInterpolator(new android.view.animation.AccelerateInterpolator()).withEndAction(remove).start();
    }

    boolean isDark(int color) {
        int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
        return (r * 299 + g * 587 + b * 114) < 128000;
    }

    void startRing() {
        stopRing();
        try {
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ring = RingtoneManager.getRingtone(this, u);
            ring.play();
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createWaveform(new long[]{0, 400, 200, 400}, -1));
            h.postDelayed(this::stopRing, 8000);
        } catch (Exception e) { }
    }

    void stopRing() { try { if (ring != null) ring.stop(); } catch (Exception e) { } ring = null; }

    @Override public void onDestroy() {
        hideGlassAlert(false);
        h.removeCallbacksAndMessages(null);
        if (bubble != null) bubble.hide();
        super.onDestroy();
    }
}