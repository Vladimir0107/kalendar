package ru.planner.zadachi;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
