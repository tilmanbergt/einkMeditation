package eu.embodyagile.bodhisattvafriend.history;

public class MeditationInsights {

    public final String languageTag; // e.g. "de", "en", "de-DE"


    public final int goalMinutesPerDay;

    public final long minutesToday;

    public final int currentStreakDays;     // streak including today

    public final int yesterdayStreakDays;   // streak ending yesterday (if today has no session)

    public final boolean streakEndedYesterday;
    public final int bestStreakDays;

    // Effective window lengths (may be 7/30 OR shortened by restart logic OR <7/<30 for new users)
    public final int daysWindow1;           // “weekly horizon” effective days (usually 7, but can be 1..7)
    public final double avgMinutesPerDay1;

    public final int daysWindow2;           // “monthly horizon” effective days (usually 30, but can be 1..30)
    public final double avgMinutesPerDay2;

    // restart conditions / messaging
    public final boolean weekRestartMessageToday;   // true only when TODAY is first session after 7-day pause
    public final boolean monthRestartMessageToday;  // true only when TODAY is first session after 30-day pause

    // suggestion semantics (UI can build text + link)
    public final int suggestedMoreMinutesToday;   // “how many more minutes today”
    public final int suggestionType;              // one of MeditationInsightsRepository.SUGGESTION_*
    public final String suggestionText;           // already phrased, 1 line

    // headline (top positive framing area)
    public final String headline1;
    public final String headline2;                // optional (can be "")
    public final int suggestedNewGoalMinutes; // 0 = none

    public MeditationInsights(
            String languageTag,
            int goalMinutesPerDay,
            long minutesToday,
            int currentStreakDays,
            int yesterdayStreakDays,
            boolean streakEndedYesterday,
            int bestStreakDays,
            int daysWindow1,
            double avgMinutesPerDay1,
            int daysWindow2,
            double avgMinutesPerDay2,
            boolean weekRestartMessageToday,
            boolean monthRestartMessageToday,
            int suggestedMoreMinutesToday,
            int suggestionType,
            int suggestedNewGoalMinutes,   // <--- NEU
            String suggestionText,
            String headline1,
            String headline2
    ) {
        this.languageTag = languageTag;
        this.goalMinutesPerDay = goalMinutesPerDay;
        this.minutesToday = minutesToday;
        this.currentStreakDays = currentStreakDays;
        this.yesterdayStreakDays = yesterdayStreakDays;
        this.streakEndedYesterday = streakEndedYesterday;
        this.bestStreakDays = bestStreakDays;
        this.daysWindow1 = daysWindow1;
        this.avgMinutesPerDay1 = avgMinutesPerDay1;
        this.daysWindow2 = daysWindow2;
        this.avgMinutesPerDay2 = avgMinutesPerDay2;
        this.weekRestartMessageToday = weekRestartMessageToday;
        this.monthRestartMessageToday = monthRestartMessageToday;
        this.suggestedMoreMinutesToday = suggestedMoreMinutesToday;
        this.suggestionType = suggestionType;
        this.suggestedNewGoalMinutes = Math.max(0, suggestedNewGoalMinutes); // <--- NEU
        this.suggestionText = suggestionText;
        this.headline1 = headline1;
        this.headline2 = headline2;
    }
}
