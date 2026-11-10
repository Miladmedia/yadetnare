package ir.yadavar;

import android.content.Context;
import java.util.*;
import org.json.*;

class Store {
    static List<Reminder> all(Context c) {
        List<Reminder> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(U.sp(c).getString("items", "[]"));
            for (int i = 0; i < a.length(); i++) l.add(Reminder.from(a.getJSONObject(i)));
        } catch (Exception e) { }
        Collections.sort(l, (x, y) -> {
            if (x.d == 0 && y.d == 0) return Long.compare(x.id, y.id);
            if (x.d == 0) return 1;
            if (y.d == 0) return -1;
            return Long.compare(x.d, y.d);
        });
        return l;
    }

    static void save(Context c, List<Reminder> l) {
        JSONArray a = new JSONArray();
        try { for (Reminder r : l) a.put(r.json()); } catch (Exception e) { }
        U.sp(c).edit().putString("items", a.toString()).apply();
    }

    static void add(Context c, Reminder r) {
        List<Reminder> l = all(c);
        l.add(r);
        save(c, l);
    }

    static void remove(Context c, long id) {
        List<Reminder> l = all(c);
        for (Iterator<Reminder> i = l.iterator(); i.hasNext(); ) if (i.next().id == id) i.remove();
        save(c, l);
    }

    static void snooze(Context c, long id, long until) {
        List<Reminder> l = all(c);
        for (Reminder r : l) if (r.id == id) r.snooze = until;
        save(c, l);
    }

    static Reminder make(Context c, String t, String n, int baseMin, long d, long start) {
        Reminder r = new Reminder();
        r.id = System.currentTimeMillis();
        r.t = t; r.n = n; r.base = Math.max(1, baseMin); r.d = d;
        r.start = start == 0 ? r.id : start;
        return r;
    }
}
