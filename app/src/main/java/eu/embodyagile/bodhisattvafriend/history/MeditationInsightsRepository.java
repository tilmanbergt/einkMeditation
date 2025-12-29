package eu.embodyagile.bodhisattvafriend.history;

import android.content.Context;

import java.util.*;

import eu.embodyagile.bodhisattvafriend.R;
import eu.embodyagile.bodhisattvafriend.settings.AppSettings;

public class MeditationInsightsRepository {

    // 95% threshold for 👍 “on track”
    public static final double TOL = 0.05; // 5%

    // cap extra suggestion to 1.5x daily goal total for today
    public static final double EXTRA_CAP_MULTIPLIER = 1.5;

    // suggestion types (for click actions / future tuning)
    public static final int SUGGESTION_NONE = 0;
    public static final int SUGGESTION_DAILY = 1;
    public static final int SUGGESTION_WEEKLY = 2;
    public static final int SUGGESTION_MONTHLY = 3;
    public static final int SUGGESTION_ADAPT_GOAL = 4;
    public static final int SUGGESTION_WASH_BOWLS = 5;

    public interface Listener {
        void onInsightsUpdated(MeditationInsights insights);
    }

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private static MeditationInsightsRepository instance;

    public static synchronized MeditationInsightsRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MeditationInsightsRepository(context.getApplicationContext());
        }
        return instance;
    }

    private final Context appContext;
    private final Set<Listener> listeners = new HashSet<>();
    private MeditationInsights cached;

    private MeditationInsightsRepository(Context appContext) {
        this.appContext = appContext;

        AppSettings.getPrefs(appContext).registerOnSharedPreferenceChangeListener((p, key) -> {
            if (AppSettings.KEY_DAILY_GOAL_MINUTES.equals(key)) {
                recompute();
            }
        });

        recompute();
    }

    public synchronized MeditationInsights getCached() {
        return cached;
    }

    public synchronized void addListener(Listener l) {
        listeners.add(l);
        if (cached != null) l.onInsightsUpdated(cached);
    }

    public synchronized void removeListener(Listener l) {
        listeners.remove(l);
    }

    public void notifySessionsChanged() {
        recompute();
    }

    public void recompute() {
        MeditationInsights computed = computeNow(appContext);

        synchronized (this) {
            cached = computed;
        }

        for (Listener l : new ArrayList<>(listeners)) {
            l.onInsightsUpdated(computed);
        }
    }

    // ----------------- Computation -----------------

    private static MeditationInsights computeNow(Context context) {
        final int goal = AppSettings.getDailyGoalMinutes(context);

        List<SessionLogEntry> sessions = SessionLogManager.getSessions(context);
        if (sessions == null) sessions = Collections.emptyList();

        // If no sessions yet: baseline snapshot
        if (sessions.isEmpty()) {
            String headline1 = context.getString(R.string.history_headline_start);
            String headline2 = context.getString(R.string.history_headline_start_2);

            int suggested = Math.min(5, Math.max(0, goal));
            String suggestion = context.getString(R.string.history_suggest_start, suggested);

            return new MeditationInsights(
                    goal,
                    0,
                    0,
                    0,
                    false,
                    0,
                    0,
                    0,
                    0,
                    0,
                    false,
                    false,
                    suggested,
                    SUGGESTION_DAILY,
                    suggestion,
                    headline1,
                    headline2
            );
        }

        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);

        long now = System.currentTimeMillis();
        long todayIndex = dayIndexOf(now, cal);

        // Aggregate minutes per dayIndex (sum across sessions in same day)
        HashMap<Long, Integer> minutesByDay = new HashMap<>();
        long firstDayIndex = Long.MAX_VALUE;

        // newest-first helps nothing here; but OK
        sessions.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

        for (SessionLogEntry e : sessions) {
            long di = dayIndexOf(e.timestamp, cal);
            if (di > todayIndex) continue;

            firstDayIndex = Math.min(firstDayIndex, di);

            int minutes = (e.actualMillis > 0) ? Math.round(e.actualMillis / 60000f) : 0;
            if (minutes <= 0) continue;

            Integer cur = minutesByDay.get(di);
            minutesByDay.put(di, (cur == null ? 0 : cur) + minutes);
        }

        if (firstDayIndex == Long.MAX_VALUE) firstDayIndex = todayIndex;

        int minutesToday = minutesByDay.getOrDefault(todayIndex, 0);

        // streak: consecutive calendar days with minutes>0
        StreakInfo streakInfo = computeStreak(minutesByDay, todayIndex);
        int bestStreak = computeLongestStreak(minutesByDay.keySet());

        // windows (weekly & monthly with “restart-aware shortening”)
        HorizonResult week = computeHorizon(minutesByDay, todayIndex, firstDayIndex, 7);
        HorizonResult month = computeHorizon(minutesByDay, todayIndex, firstDayIndex, 30);

        // restart message rule “only when today is first session after full pause”
        boolean weekRestartMessageToday = (minutesToday > 0) && isFirstSessionTodayAfterFullPause(minutesByDay, todayIndex, 7);
        boolean monthRestartMessageToday = (minutesToday > 0) && isFirstSessionTodayAfterFullPause(minutesByDay, todayIndex, 30);

        // Suggestion (includes horizon name)
        Suggestion suggestion = buildSuggestion(context, goal, minutesToday, week, month, firstDayIndex, todayIndex);

        // Headline (celebration zone)
        Headline headline = buildHeadline(context, goal, minutesToday, week, month, weekRestartMessageToday, streakInfo.currentStreakDays);

        return new MeditationInsights(
                goal,
                minutesToday,
                streakInfo.currentStreakDays,
                streakInfo.yesterdayStreakDays,
                streakInfo.streakEndedYesterday,
                bestStreak,
                week.effectiveDays,
                week.avgPerDay,
                month.effectiveDays,
                month.avgPerDay,
                weekRestartMessageToday,
                monthRestartMessageToday,
                suggestion.moreMinutesToday,
                suggestion.type,
                suggestion.text,
                headline.line1,
                headline.line2
        );
    }

    // ----------------- Horizon logic -----------------

    private static class HorizonResult {
        final int maxDays;         // 7 or 30
        final int effectiveDays;   // what we really use (may be < maxDays)
        final int totalMinutes;    // total over effectiveDays
        final double avgPerDay;

        HorizonResult(int maxDays, int effectiveDays, int totalMinutes) {
            this.maxDays = maxDays;
            this.effectiveDays = Math.max(0, effectiveDays);
            this.totalMinutes = Math.max(0, totalMinutes);
            this.avgPerDay = (this.effectiveDays > 0) ? (this.totalMinutes / (double) this.effectiveDays) : 0.0;
        }
    }

    /**
     * Computes “weekly(7)” or “monthly(30)” horizon using your exact rule:
     *
     * - Default: full window avg = total minutes in last N calendar days / N (includes zero days)
     * - BUT if there is a current consecutive run ending today AND that run started after a full pause of N days,
     *   then effectiveDays = runLength (e.g., 2 days) and avg is over that run.
     *
     * - If user has fewer than N days since first usage, effectiveDays = daysSinceFirst and avg is over those days.
     */
    private static HorizonResult computeHorizon(
            Map<Long, Integer> minutesByDay,
            long todayIndex,
            long firstDayIndex,
            int maxDays
    ) {
        int daysSinceFirst = (int) (todayIndex - firstDayIndex + 1);
        if (daysSinceFirst < 1) daysSinceFirst = 1;

        // If user is new: use daysSinceFirst (no ambiguity)
        if (daysSinceFirst < maxDays) {
            int total = sumMinutesRange(minutesByDay, todayIndex - (daysSinceFirst - 1), todayIndex);
            return new HorizonResult(maxDays, daysSinceFirst, total);
        }

        int minutesToday = minutesByDay.getOrDefault(todayIndex, 0);

        // If no meditation today, we do NOT shorten; we keep full window
        if (minutesToday <= 0) {
            int total = sumMinutesRange(minutesByDay, todayIndex - (maxDays - 1), todayIndex);
            return new HorizonResult(maxDays, maxDays, total);
        }

        // Determine run length ending today
        int runLength = consecutiveRunLengthEndingAt(minutesByDay, todayIndex);

        long runStart = todayIndex - (runLength - 1);

        // Pause length before runStart (how many consecutive zero days immediately before runStart)
        int pauseBefore = consecutivePauseBefore(minutesByDay, runStart);

        // Shorten ONLY if pauseBefore >= maxDays
        if (pauseBefore >= maxDays) {
            int totalRun = sumMinutesRange(minutesByDay, runStart, todayIndex);
            return new HorizonResult(maxDays, runLength, totalRun);
        }

        // Default: full window
        int total = sumMinutesRange(minutesByDay, todayIndex - (maxDays - 1), todayIndex);
        return new HorizonResult(maxDays, maxDays, total);
    }

    private static int consecutiveRunLengthEndingAt(Map<Long, Integer> minutesByDay, long endDay) {
        int len = 0;
        long d = endDay;
        while (minutesByDay.getOrDefault(d, 0) > 0) {
            len++;
            d--;
        }
        return Math.max(0, len);
    }

    private static int consecutivePauseBefore(Map<Long, Integer> minutesByDay, long runStartDay) {
        int pause = 0;
        long d = runStartDay - 1;
        while (minutesByDay.getOrDefault(d, 0) <= 0) {
            pause++;
            d--;
            // safety: avoid infinite loop if dataset is sparse far back
            if (pause > 4000) break;
        }
        return pause;
    }

    private static boolean isFirstSessionTodayAfterFullPause(Map<Long, Integer> minutesByDay, long todayIndex, int pauseDays) {
        // today must have minutes > 0 for this to matter
        if (minutesByDay.getOrDefault(todayIndex, 0) <= 0) return false;

        // check that the previous 'pauseDays' days (yesterday back) are all zero
        for (int i = 1; i <= pauseDays; i++) {
            if (minutesByDay.getOrDefault(todayIndex - i, 0) > 0) return false;
        }
        return true;
    }

    private static int sumMinutesRange(Map<Long, Integer> minutesByDay, long fromDay, long toDay) {
        int sum = 0;
        for (long d = fromDay; d <= toDay; d++) {
            sum += minutesByDay.getOrDefault(d, 0);
        }
        return sum;
    }

    // ----------------- Suggestion -----------------

    private static class Suggestion {
        final int moreMinutesToday;
        final int type;
        final String text;

        Suggestion(int moreMinutesToday, int type, String text) {
            this.moreMinutesToday = Math.max(0, moreMinutesToday);
            this.type = type;
            this.text = text;
        }
    }

    private static final int MIN_SUGGESTION_MINUTES = 5; // easy to tweak later

    private static Suggestion buildSuggestion(
            Context context,
            int goal,
            int minutesToday,
            HorizonResult week,
            HorizonResult month,
            long firstDayIndex,
            long todayIndex
    ) {
        int g = Math.max(0, goal);
        if (g <= 0) {
            return new Suggestion(10, SUGGESTION_DAILY,
                    context.getString(R.string.history_suggest_daily, 10));
        }

        // daily on track requires actual practice today (per your definition)
        boolean dailyOnTrack95 = (minutesToday > 0) && (minutesToday >= Math.round(g * (1.0 - TOL)));

        boolean weekOnTrack95 = (week.avgPerDay >= g * (1.0 - TOL));
        boolean monthOnTrack95 = (month.avgPerDay >= g * (1.0 - TOL));

        boolean dailyMet100 = (minutesToday >= g);
        boolean weekMet100 = (week.avgPerDay >= g);
        boolean monthMet100 = (month.avgPerDay >= g);

        int daysSinceFirst = (int) (todayIndex - firstDayIndex + 1);

        // Goal adaptation: only when FULL 30-day window exists and progress is very low
        boolean full30Available = (daysSinceFirst >= 30) && (month.effectiveDays == 30);
        if (full30Available) {
            double monthProgress = (month.totalMinutes / (double) (g * 30));
            boolean veryLow = monthProgress < 0.5;

            if (veryLow && !dailyOnTrack95 && !weekOnTrack95 && !monthOnTrack95) {
                return new Suggestion(0, SUGGESTION_ADAPT_GOAL,
                        context.getString(R.string.history_suggest_adapt_goal));
            }
        }

        // Cap: total today should not exceed 1.5x goal
        int capTotal = (int) Math.ceil(g * EXTRA_CAP_MULTIPLIER);
        int maxExtra = Math.max(0, capTotal - minutesToday);

        // Helper: apply minimum suggestion (but never exceed cap)
        java.util.function.IntUnaryOperator applyMin = (raw) -> {
            if (raw <= 0) return 0;
            int v = Math.max(raw, MIN_SUGGESTION_MINUTES);
            return Math.min(v, maxExtra);
        };

        // Monthly line is meaningful only if >=10 effective days
        boolean showMonthlyLine = month.effectiveDays >= 10;

        // --- NEW RULE (your improvement b) ---
        // If daily is already "good enough" (>=95%) and weekly/monthly are NOT ( <95% ),
        // prefer suggesting minutes to improve weekly/monthly (more motivating than tiny daily top-up).
        if (dailyOnTrack95) {
            if (!weekOnTrack95) {
                int targetTotal = g * week.effectiveDays;                 // bring avg to 100% across effective window
                int neededToday = Math.max(0, targetTotal - week.totalMinutes);
                int suggest = applyMin.applyAsInt(neededToday);

                if (suggest > 0) {
                    return new Suggestion(suggest, SUGGESTION_WEEKLY,
                            context.getString(R.string.history_suggest_weekly, suggest));
                }
            }

            if (showMonthlyLine && !monthOnTrack95) {
                int targetTotal = g * month.effectiveDays;
                int neededToday = Math.max(0, targetTotal - month.totalMinutes);
                int suggest = applyMin.applyAsInt(neededToday);

                if (suggest > 0) {
                    return new Suggestion(suggest, SUGGESTION_MONTHLY,
                            context.getString(R.string.history_suggest_monthly, suggest));
                }
            }
        }

        // --- Existing ladder (slightly simplified) ---
        // 1) Daily below 100 -> suggest remaining to reach daily target
        if (!dailyMet100) {
            int need = Math.max(0, g - minutesToday);
            int suggest = applyMin.applyAsInt(need);

            if (suggest <= 0) return new Suggestion(0, SUGGESTION_NONE, "");

            if (weekMet100 && monthMet100) {
                return new Suggestion(suggest, SUGGESTION_DAILY,
                        context.getString(R.string.history_suggest_daily_keep_momentum, suggest));
            } else {
                return new Suggestion(suggest, SUGGESTION_DAILY,
                        context.getString(R.string.history_suggest_daily, suggest));
            }
        }

        // 2) Weekly to 100 (only if daily is at least on track)
        if (dailyOnTrack95 && !weekMet100) {
            int targetTotal = g * week.effectiveDays;
            int neededToday = Math.max(0, targetTotal - week.totalMinutes);
            int suggest = applyMin.applyAsInt(neededToday);

            if (suggest > 0) {
                return new Suggestion(suggest, SUGGESTION_WEEKLY,
                        context.getString(R.string.history_suggest_weekly, suggest));
            }
        }

        // 3) Monthly to 100 (only if daily+weekly are on track)
        if (showMonthlyLine && dailyOnTrack95 && weekOnTrack95 && !monthMet100) {
            int targetTotal = g * month.effectiveDays;
            int neededToday = Math.max(0, targetTotal - month.totalMinutes);
            int suggest = applyMin.applyAsInt(neededToday);

            if (suggest > 0) {
                return new Suggestion(suggest, SUGGESTION_MONTHLY,
                        context.getString(R.string.history_suggest_monthly, suggest));
            }
        }

        // 4) Everything on track -> wash bowls
        if (dailyOnTrack95 && weekOnTrack95 && (!showMonthlyLine || monthOnTrack95)) {
            return new Suggestion(0, SUGGESTION_WASH_BOWLS,
                    context.getString(R.string.history_suggest_wash_bowls));
        }

        return new Suggestion(0, SUGGESTION_NONE, "");
    }


    // ----------------- Headline -----------------

    private static class Headline {
        final String line1;
        final String line2;

        Headline(String line1, String line2) {
            this.line1 = line1;
            this.line2 = line2 == null ? "" : line2;
        }
    }

    private static Headline buildHeadline(
            Context context,
            int goal,
            int minutesToday,
            HorizonResult week,
            HorizonResult month,
            boolean weekRestartMessageToday,
            int currentStreakDays
    ) {
        int g = Math.max(0, goal);
        if (g <= 0) {
            return new Headline(
                    context.getString(R.string.history_headline_generic),
                    ""
            );
        }

        double pToday = minutesToday / (double) g;
        double pWeek = week.avgPerDay / (double) g;
        double pMonth = month.avgPerDay / (double) g;

        boolean showMonth = month.effectiveDays >= 10;

        // Celebration: choose the horizon with the largest progress above 100%
        double bestP = -1;
        int best = -1; // 0=today 1=week 2=month

        if (pToday >= 1.0) { bestP = pToday; best = 0; }
        if (pWeek > bestP && pWeek >= 1.0) { bestP = pWeek; best = 1; }
        if (showMonth && pMonth > bestP && pMonth >= 1.0) { bestP = pMonth; best = 2; }
        // tie-break: if equal and month exists, prefer month
        if (showMonth && pMonth == bestP && pMonth >= 1.0) best = 2;

        if (best == 2) {
            return new Headline(
                    context.getString(R.string.history_headline_month_on_fire),
                    ""
            );
        }
        if (best == 1) {
            if (minutesToday <= 0) {
                return new Headline(
                        context.getString(R.string.history_headline_week_on_track),
                        context.getString(R.string.history_headline_continue_today)
                );
            }
            return new Headline(
                    context.getString(R.string.history_headline_week_on_track),
                    ""
            );
        }
        if (best == 0) {
            return new Headline(
                    context.getString(R.string.history_headline_today_done),
                    ""
            );
        }

        // No >=100 celebration -> restart or “on track” signals
        if (weekRestartMessageToday) {
            return new Headline(
                    context.getString(R.string.history_headline_restart),
                    ""
            );
        }

        boolean weekOnTrack95 = (pWeek >= (1.0 - TOL));
        boolean monthOnTrack95 = showMonth && (pMonth >= (1.0 - TOL));

        if (monthOnTrack95) {
            return new Headline(context.getString(R.string.history_headline_month_on_track), "");
        }
        if (weekOnTrack95) {
            if (minutesToday <= 0) {
                return new Headline(
                        context.getString(R.string.history_headline_week_on_track),
                        context.getString(R.string.history_headline_continue_today)
                );
            }
            return new Headline(context.getString(R.string.history_headline_week_on_track), "");
        }

        if (minutesToday > 0) {
            return new Headline(context.getString(R.string.history_headline_today_good_start), "");
        }

        // Streak is low priority; only mention long streaks later (we keep it simple here)
        if (currentStreakDays >= 10 && currentStreakDays % 10 == 0) {
            return new Headline(context.getString(R.string.history_headline_streak_milestone), "");
        }

        return new Headline(context.getString(R.string.history_headline_generic), "");
    }

    // ----------------- Streak helpers -----------------

    private static class StreakInfo {
        final int currentStreakDays;        // streak including today (if today has minutes)
        final int yesterdayStreakDays;      // streak ending yesterday (if today has 0)
        final boolean streakEndedYesterday; // true if yesterdayStreakDays >= 2

        StreakInfo(int currentStreakDays, int yesterdayStreakDays, boolean streakEndedYesterday) {
            this.currentStreakDays = currentStreakDays;
            this.yesterdayStreakDays = yesterdayStreakDays;
            this.streakEndedYesterday = streakEndedYesterday;
        }
    }

    private static StreakInfo computeStreak(Map<Long, Integer> minutesByDay, long todayIndex) {
        // streak including today?
        int streak = 0;
        long d = todayIndex;
        while (minutesByDay.getOrDefault(d, 0) > 0) {
            streak++;
            d--;
        }
        if (streak >= 1) {
            return new StreakInfo(streak, 0, false);
        }

        // streak ending yesterday (only relevant if today has 0)
        int yStreak = 0;
        d = todayIndex - 1;
        while (minutesByDay.getOrDefault(d, 0) > 0) {
            yStreak++;
            d--;
        }

        boolean endedYesterday = yStreak >= 2;
        return new StreakInfo(0, yStreak, endedYesterday);
    }


    private static int computeLongestStreak(Set<Long> dayIndices) {
        if (dayIndices == null || dayIndices.isEmpty()) return 0;

        List<Long> days = new ArrayList<>(dayIndices);
        Collections.sort(days);

        int best = 1;
        int cur = 1;

        for (int i = 1; i < days.size(); i++) {
            if (days.get(i) == days.get(i - 1) + 1) {
                cur++;
                best = Math.max(best, cur);
            } else {
                cur = 1;
            }
        }
        return best;
    }

    // ----------------- Date helpers -----------------

    private static long dayIndexOf(long timestampMs, Calendar cal) {
        cal.setTimeInMillis(timestampMs);
        setToStartOfDay(cal);
        long start = cal.getTimeInMillis();
        return start / DAY_MS;
    }

    private static void setToStartOfDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }
}
