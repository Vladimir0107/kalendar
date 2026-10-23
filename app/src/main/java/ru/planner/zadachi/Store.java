package ru.planner.zadachi;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Local storage for tasks and the work-shift schedule (SharedPreferences + JSON). */
public class Store {

    private static final String PREFS = "planner";

    public static class Task {
        public String text;
        public boolean done;

        public Task(String text, boolean done) {
            this.text = text;
            this.done = done;
        }
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- tasks ----------

    public static List<Task> getTasks(Context c, LocalDate d) {
        List<Task> out = new ArrayList<>();
        String s = sp(c).getString("t_" + d, null);
        if (s == null) return out;
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new Task(o.optString("t"), o.optBoolean("d")));
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    public static void saveTasks(Context c, LocalDate d, List<Task> tasks) {
        if (tasks.isEmpty()) {
            sp(c).edit().remove("t_" + d).apply();
            return;
        }
        JSONArray a = new JSONArray();
        for (Task t : tasks) {
            JSONObject o = new JSONObject();
            try {
                o.put("t", t.text);
                o.put("d", t.done);
            } catch (JSONException ignored) {
            }
            a.put(o);
        }
        sp(c).edit().putString("t_" + d, a.toString()).apply();
    }

    // ---------- work shifts ----------

    /** Pattern: `work` working days followed by `off` days off, cycle starts at `start`. */
    public static void setShift(Context c, int work, int off, LocalDate start) {
        SharedPreferences.Editor e = sp(c).edit();
        removeOverrides(c, e);
        e.putInt("sw", work).putInt("so", off).putLong("ss", start.toEpochDay()).apply();
    }

    public static void clearShift(Context c) {
        SharedPreferences.Editor e = sp(c).edit();
        removeOverrides(c, e);
        e.remove("sw").remove("so").remove("ss").apply();
    }

    private static void removeOverrides(Context c, SharedPreferences.Editor e) {
        for (String k : sp(c).getAll().keySet()) {
            if (k.startsWith("o_")) e.remove(k);
        }
    }

    public static boolean hasShift(Context c) {
        return sp(c).getInt("sw", 0) > 0;
    }

    public static String shiftDescription(Context c) {
        if (!hasShift(c)) return "не задан";
        SharedPreferences p = sp(c);
        return p.getInt("sw", 0) + "/" + p.getInt("so", 0)
                + ", с " + LocalDate.ofEpochDay(p.getLong("ss", 0));
    }

    private static boolean patternWork(Context c, LocalDate d) {
        SharedPreferences p = sp(c);
        int work = p.getInt("sw", 0);
        if (work <= 0) return false;
        int off = p.getInt("so", 0);
        long start = p.getLong("ss", 0);
        long diff = d.toEpochDay() - start;
        long mod = Math.floorMod(diff, (long) (work + off));
        return mod < work;
    }

    public static boolean isWorkDay(Context c, LocalDate d) {
        String k = "o_" + d;
        SharedPreferences p = sp(c);
        if (p.contains(k)) return p.getBoolean(k, false);
        return patternWork(c, d);
    }

    // ---------- worked hours / pieces per day ----------

    public static class Work {
        public String in;      // "HH:mm" or null
        public String out;     // "HH:mm" or null
        public double hours;
        public double pieces;

        public boolean isEmpty() {
            return in == null && out == null && hours == 0 && pieces == 0;
        }
    }

    public static Work getWork(Context c, LocalDate d) {
        Work w = new Work();
        String s = sp(c).getString("w_" + d, null);
        if (s == null) return w;
        try {
            JSONObject o = new JSONObject(s);
            w.in = o.has("in") ? o.getString("in") : null;
            w.out = o.has("out") ? o.getString("out") : null;
            w.hours = o.optDouble("h", 0);
            w.pieces = o.optDouble("p", 0);
        } catch (JSONException ignored) {
        }
        return w;
    }

    public static void saveWork(Context c, LocalDate d, Work w) {
        if (w.isEmpty()) {
            sp(c).edit().remove("w_" + d).apply();
            return;
        }
        JSONObject o = new JSONObject();
        try {
            if (w.in != null) o.put("in", w.in);
            if (w.out != null) o.put("out", w.out);
            o.put("h", w.hours);
            o.put("p", w.pieces);
        } catch (JSONException ignored) {
        }
        sp(c).edit().putString("w_" + d, o.toString()).apply();
    }

    /** Hours between two "HH:mm" times; handles night shifts; subtracts unpaid break. */
    public static double hoursBetween(String in, String out, int breakMin) {
        try {
            int a = LocalTime.parse(in).toSecondOfDay() / 60;
            int b = LocalTime.parse(out).toSecondOfDay() / 60;
            int m = b - a;
            if (m <= 0) m += 24 * 60;
            m = Math.max(0, m - breakMin);
            return Math.round(m / 60.0 * 100) / 100.0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ---------- pay settings ----------

    public static boolean isPiecePay(Context c) {
        return "piece".equals(sp(c).getString("pay_type", "hour"));
    }

    public static void setPiecePay(Context c, boolean piece) {
        sp(c).edit().putString("pay_type", piece ? "piece" : "hour").apply();
    }

    public static double getD(Context c, String key) {
        return sp(c).getFloat(key, 0f);
    }

    public static void setD(Context c, String key, double v) {
        sp(c).edit().putFloat(key, (float) v).apply();
    }

    // keys for getD / setD
    public static final String RATE_HOUR = "rate_hour";     // money per hour
    public static final String RATE_PIECE = "rate_piece";   // money per piece
    public static final String PLAN_HOURS = "plan_hours";   // planned hours per shift
    public static final String PLAN_PIECES = "plan_pieces"; // planned pieces per shift
    public static final String BREAK_MIN = "break_min";     // unpaid break, minutes
    public static final String MAX_MONTH = "max_month";     // manual max salary (0 = auto)

    public static double dayPay(Context c, Work w) {
        if (isPiecePay(c)) return w.pieces * getD(c, RATE_PIECE);
        return w.hours * getD(c, RATE_HOUR);
    }

    /** Max possible salary for a month: manual value, or shifts x plan x rate. */
    public static double maxForMonth(Context c, int shiftDays) {
        double manual = getD(c, MAX_MONTH);
        if (manual > 0) return manual;
        if (isPiecePay(c)) return shiftDays * getD(c, PLAN_PIECES) * getD(c, RATE_PIECE);
        return shiftDays * getD(c, PLAN_HOURS) * getD(c, RATE_HOUR);
    }

    // ---------- formatting ----------

    public static final Locale RU = new Locale("ru");

    public static String money(double v) {
        return String.format(RU, "%,.0f ₽", v);
    }

    public static String num(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        String s = String.format(Locale.US, "%.2f", v);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public static double parseNum(String s) {
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (Exception e) {
            return 0;
        }
    }

    /** Manually mark a single day as work / day off (overrides the pattern). */
    public static void setWorkOverride(Context c, LocalDate d, boolean work) {
        String k = "o_" + d;
        if (work == patternWork(c, d)) {
            sp(c).edit().remove(k).apply();
        } else {
            sp(c).edit().putBoolean(k, work).apply();
        }
    }
}
