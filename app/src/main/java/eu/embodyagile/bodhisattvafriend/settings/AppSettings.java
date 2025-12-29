package eu.embodyagile.bodhisattvafriend.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppSettings {

    private AppSettings() {}

    // ✅ single source of truth
    public static final String PREFS_NAME = "bodhisattva_friend_prefs";


    // --- Keys ---
    public static final String KEY_DAILY_GOAL_MINUTES = "daily_goal_avg_minutes";
    public static final String KEY_PRE_MEDITATION_COUNTDOWN = "pre_meditation_countdown";

    public static final String KEY_DND_ENABLED = "dnd_enabled";
    public static final String KEY_CANDLE_ENABLED = "candle_enabled";
    public static final String KEY_FLASH_ON_START_END = "flash_on_start_end";
    public static final String KEY_VIBRATION_ON_START_END = "vibration_on_start_end";

    private static final String KEY_LAST_DURATION_PREFIX = "last_duration_";

    // --- Defaults / bounds ---
    private static final int DEFAULT_DAILY_GOAL_MINUTES = 60;
    private static final int MIN_DAILY_GOAL_MINUTES = 5;
    private static final int MAX_DAILY_GOAL_MINUTES = 180;

    private static final int DEFAULT_PRE_MEDITATION_COUNTDOWN = 0; // seconds
    private static final int MIN_PRE_MEDITATION_COUNTDOWN = 0;
    private static final int MAX_PRE_MEDITATION_COUNTDOWN = 3600;

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getPrefs(Context c) {
        return prefs(c);
    }

    // ---------------------------
    // One-time migration (IMPORTANT)
    // ---------------------------
    /** Call once on app start (e.g. in BaseActivity.onCreate or Application.onCreate). */


    // ---------------------------
    // Daily goal (avg minutes/day)
    // ---------------------------
    public static int getDailyGoalMinutes(Context c) {
        int v = prefs(c).getInt(KEY_DAILY_GOAL_MINUTES, DEFAULT_DAILY_GOAL_MINUTES);
        return clamp(v, MIN_DAILY_GOAL_MINUTES, MAX_DAILY_GOAL_MINUTES);
    }

    public static void setDailyGoalMinutes(Context c, int minutes) {
        int v = clamp(minutes, MIN_DAILY_GOAL_MINUTES, MAX_DAILY_GOAL_MINUTES);
        prefs(c).edit().putInt(KEY_DAILY_GOAL_MINUTES, v).apply();
    }

    // ---------------------------
    // Premeditation countdown (sec)
    // ---------------------------
    public static int getPremeditationCountdownSec(Context c) {
        int v = prefs(c).getInt(KEY_PRE_MEDITATION_COUNTDOWN, DEFAULT_PRE_MEDITATION_COUNTDOWN);
        return clamp(v, MIN_PRE_MEDITATION_COUNTDOWN, MAX_PRE_MEDITATION_COUNTDOWN);
    }

    public static void setPremeditationCountdownSec(Context c, int seconds) {
        int v = clamp(seconds, MIN_PRE_MEDITATION_COUNTDOWN, MAX_PRE_MEDITATION_COUNTDOWN);
        prefs(c).edit().putInt(KEY_PRE_MEDITATION_COUNTDOWN, v).apply();
    }

    // ---------------------------
    // Toggles used by MeditationActivity
    // ---------------------------
    public static boolean isDndEnabled(Context c) {
        return prefs(c).getBoolean(KEY_DND_ENABLED, false);
    }

    public static void setDndEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean(KEY_DND_ENABLED, enabled).apply();
    }

    public static boolean isCandleEnabled(Context c) {
        return prefs(c).getBoolean(KEY_CANDLE_ENABLED, true);
    }

    public static void setCandleEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean(KEY_CANDLE_ENABLED, enabled).apply();
    }

    public static boolean isFlashOnStartEndEnabled(Context c) {
        return prefs(c).getBoolean(KEY_FLASH_ON_START_END, false);
    }

    public static void setFlashOnStartEndEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean(KEY_FLASH_ON_START_END, enabled).apply();
    }

    public static boolean isVibrationOnStartEndEnabled(Context c) {
        return prefs(c).getBoolean(KEY_VIBRATION_ON_START_END, false);
    }

    public static void setVibrationOnStartEndEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean(KEY_VIBRATION_ON_START_END, enabled).apply();
    }

    // ---------------------------
    // Last-used duration per practice
    // ---------------------------
    public static int getLastDurationMinutes(Context c, String practiceId, int fallbackMinutes) {
        if (practiceId == null) return fallbackMinutes;
        return prefs(c).getInt(KEY_LAST_DURATION_PREFIX + practiceId, fallbackMinutes);
    }

    public static void setLastDurationMinutes(Context c, String practiceId, int minutes) {
        if (practiceId == null) return;
        int v = Math.max(1, minutes);
        prefs(c).edit().putInt(KEY_LAST_DURATION_PREFIX + practiceId, v).apply();
    }

    // ---------------------------
    // helpers
    // ---------------------------
    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
