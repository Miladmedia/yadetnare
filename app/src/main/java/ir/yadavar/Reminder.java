package ir.yadavar;

import org.json.JSONObject;

class Reminder {
    long id, d, start, last, snooze;
    String t = "", n = "";
    int base = 15; // minutes

    JSONObject json() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id).put("t", t).put("n", n).put("d", d).put("b", base).put("s", start).put("l", last).put("z", snooze);
        return o;
    }

    static Reminder from(JSONObject o) {
        Reminder r = new Reminder();
        r.id = o.optLong("id"); r.t = o.optString("t"); r.n = o.optString("n"); r.d = o.optLong("d");
        r.base = o.optInt("b", 15); r.start = o.optLong("s"); r.last = o.optLong("l"); r.snooze = o.optLong("z");
        return r;
    }

    long eff(long now) {
        long b = base * 60000L;
        if (d == 0) return b;
        long rem = d - now;
        if (rem <= 0) return Math.min(b, 120000L);
        return Math.max(60000L, Math.min(b, rem / 6));
    }

    long next(long now) {
        long a = last == 0 ? start : last + eff(now);
        return Math.max(a, snooze);
    }
}
