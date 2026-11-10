package ir.yadavar;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
import java.util.List;

public class QuickWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { update(c); }

    static void update(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, QuickWidget.class));
        if (ids.length == 0) return;
        List<Reminder> l = Store.all(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        v.setTextViewText(R.id.title, U.t(c, "یادت نره!", "Don't forget!") + (l.isEmpty() ? "" : "  (" + U.n(c, l.size()) + ")"));
        int[] rows = {R.id.l1, R.id.l2, R.id.l3, R.id.l4, R.id.l5};
        for (int i = 0; i < rows.length; i++) {
            String s = "";
            if (i < l.size()) { Reminder r = l.get(i); String lf = U.left(c, r.d); s = "• " + r.t + (lf.isEmpty() ? "" : " — " + lf); }
            else if (i == 0) s = U.t(c, "کاری باقی نمانده 🎉", "Nothing left 🎉");
            v.setTextViewText(rows[i], s);
        }
        Intent q = new Intent(c, MainActivity.class).setAction(MainActivity.ACTION_QUICK);
        v.setOnClickPendingIntent(R.id.add, PendingIntent.getActivity(c, 1, q, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 2, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE));
        m.updateAppWidget(ids, v);
    }
}
