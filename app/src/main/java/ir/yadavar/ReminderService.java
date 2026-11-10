package ir.yadavar;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
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
            if (!ACT_TICK.equals(in.getAction())) stopRing();
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
        if (alertNow) startRing();

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
        h.removeCallbacksAndMessages(null);
        if (bubble != null) bubble.hide();
        super.onDestroy();
    }
}
