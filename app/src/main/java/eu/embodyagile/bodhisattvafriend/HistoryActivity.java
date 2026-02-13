package eu.embodyagile.bodhisattvafriend;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import java.util.List;

import eu.embodyagile.bodhisattvafriend.history.MeditationInsights;
import eu.embodyagile.bodhisattvafriend.history.MeditationInsightsRepository;
import eu.embodyagile.bodhisattvafriend.history.SessionLogEntry;
import eu.embodyagile.bodhisattvafriend.history.SessionLogManager;
import eu.embodyagile.bodhisattvafriend.settings.AppSettings;

public class HistoryActivity extends BaseActivity {

    public static final String EXTRA_SUGGESTED_MINUTES = "extra_suggested_minutes";

    private TextView tvHeadline1;
    private TextView tvHeadline2;

    private TextView tvTodayLabel, tvTodayValue, tvTodayStatus;
    private TextView tvWeekLabel, tvWeekValue, tvWeekStatus;
    private TextView tvMonthLabel, tvMonthValue, tvMonthStatus;

    private LinearLayout rowMonth;

    private TextView tvSuggestion;
    private TextView tvStreak;

    private Button btnShowList;


    private MeditationInsightsRepository repo;
    private final MeditationInsightsRepository.Listener listener = this::render;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        setupFooter(FooterTab.HISTORY);

        tvHeadline1 = findViewById(R.id.text_history_headline1);
        tvHeadline2 = findViewById(R.id.text_history_headline2);

        tvTodayLabel = findViewById(R.id.text_row_today_label);
        tvTodayValue = findViewById(R.id.text_row_today_value);
        tvTodayStatus = findViewById(R.id.text_row_today_status);

        tvWeekLabel = findViewById(R.id.text_row_week_label);
        tvWeekValue = findViewById(R.id.text_row_week_value);
        tvWeekStatus = findViewById(R.id.text_row_week_status);

        tvMonthLabel = findViewById(R.id.text_row_month_label);
        tvMonthValue = findViewById(R.id.text_row_month_value);
        tvMonthStatus = findViewById(R.id.text_row_month_status);

        rowMonth = findViewById(R.id.row_month);

        tvSuggestion = findViewById(R.id.text_history_suggestion);
        tvStreak = findViewById(R.id.text_history_streak);
        repo = MeditationInsightsRepository.getInstance(this);





        tvSuggestion.setOnClickListener(v -> handleSuggestionClick(repo.getCached()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (repo != null) repo.addListener(listener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (repo != null) repo.removeListener(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MeditationInsights cached = (repo != null) ? repo.getCached() : null;
        if (cached != null) render(cached);
    }

    private void render(MeditationInsights i) {
        List<SessionLogEntry> sessions =
                SessionLogManager.getSessions(this);

        Log.d("HISTORY_CHECK",
                "sessions found in HistoryActivity: "
                        + (sessions == null ? "null" : sessions.size()));
        if (i == null) return;

        final int goal = i.goalMinutesPerDay;
        final String thumbsUp = "👍";
        final String check = "✅";

        // --- Celebration zone ---
        tvHeadline1.setText(i.headline1 != null ? i.headline1 : "");
        tvHeadline2.setText(i.headline2 != null ? i.headline2 : "");
        tvHeadline2.setVisibility((i.headline2 != null && !i.headline2.trim().isEmpty()) ? View.VISIBLE : View.GONE);

        // --- Today row ---
        tvTodayLabel.setText(getString(R.string.history_label_today));
        if (i.minutesToday > 0) {
            tvTodayValue.setText(i.minutesToday + " " + getString(R.string.minuten));
            tvTodayStatus.setText((goal > 0 && i.minutesToday >= Math.round(goal * (1.0 - MeditationInsightsRepository.TOL)))
                    ? thumbsUp : check);
        } else {
            tvTodayValue.setText(getString(R.string.history_not_yet));
            tvTodayStatus.setText("");
        }

        // --- Week row label uses effective days in LEFT column ---
        tvWeekLabel.setText(getString(R.string.history_label_days, i.daysWindow1));

        if (i.weekRestartMessageToday) {
            tvWeekValue.setText(getString(R.string.history_restart_week_message));
            tvWeekStatus.setText("");
        } else if (i.daysWindow1 <= 1) {
            // avoid meaningless averages
            tvWeekValue.setText(getString(R.string.history_first_day_short));
            tvWeekStatus.setText("");
        } else {
            tvWeekValue.setText(getString(R.string.history_avg_minutes, Math.round(i.avgMinutesPerDay1)));
            if (goal > 0 && i.avgMinutesPerDay1 >= goal * (1.0 - MeditationInsightsRepository.TOL)) {
                tvWeekStatus.setText(thumbsUp);
            } else {
                tvWeekStatus.setText("");
            }
        }

        // --- Month row shown only if effective days >= 10 ---
        boolean showMonth = (i.daysWindow2 >= 10);
        rowMonth.setVisibility(showMonth ? View.VISIBLE : View.GONE);

        if (showMonth) {
            tvMonthLabel.setText(getString(R.string.history_label_days, i.daysWindow2));

            if (i.monthRestartMessageToday) {
                // usually suppressed by week messaging, but keep safe
                tvMonthValue.setText(getString(R.string.history_restart_month_message));
                tvMonthStatus.setText("");
            } else {
                tvMonthValue.setText(getString(R.string.history_avg_minutes, Math.round(i.avgMinutesPerDay2)));
                if (goal > 0 && i.avgMinutesPerDay2 >= goal * (1.0 - MeditationInsightsRepository.TOL)) {
                    tvMonthStatus.setText(thumbsUp);
                } else {
                    tvMonthStatus.setText("");
                }
            }
        }

        // --- Suggestion row ---
        boolean hasSuggestion = (i.suggestionText != null && !i.suggestionText.trim().isEmpty());
        if (hasSuggestion) {
            tvSuggestion.setText(i.suggestionText);
            tvSuggestion.setVisibility(View.VISIBLE);

            // enable click only for actionable types
            boolean actionable =
                    i.suggestionType == MeditationInsightsRepository.SUGGESTION_DAILY
                            || i.suggestionType == MeditationInsightsRepository.SUGGESTION_WEEKLY
                            || i.suggestionType == MeditationInsightsRepository.SUGGESTION_MONTHLY
                            || i.suggestionType == MeditationInsightsRepository.SUGGESTION_ADAPT_GOAL
                            || i.suggestionType == MeditationInsightsRepository.SUGGESTION_RAISE_GOAL;

            tvSuggestion.setClickable(actionable);
            tvSuggestion.setEnabled(actionable);
        } else {
            tvSuggestion.setText("");
            tvSuggestion.setVisibility(View.GONE);
            tvSuggestion.setClickable(false);
            tvSuggestion.setEnabled(false);
        }


        // --- Streak row ---
        if (i.currentStreakDays >= 2) {
            tvStreak.setText(getString(R.string.history_streak_days, i.currentStreakDays));
        } else if (i.streakEndedYesterday && i.yesterdayStreakDays >= 2) {
            tvStreak.setText(getString(R.string.history_streak_continue_today, i.yesterdayStreakDays));
        } else if (i.bestStreakDays >= 2) {
            tvStreak.setText(getString(R.string.history_streak_start_best, i.bestStreakDays));
        } else {
            tvStreak.setText(getString(R.string.history_streak_start));
        }

    }

    private void handleSuggestionClick(MeditationInsights i) {
        if (i == null) return;

        if (i.suggestionType == MeditationInsightsRepository.SUGGESTION_DAILY
                || i.suggestionType == MeditationInsightsRepository.SUGGESTION_WEEKLY
                || i.suggestionType == MeditationInsightsRepository.SUGGESTION_MONTHLY) {

            // Step 1: jump to MeditationActivity; Step 2: implement EXTRA handling there.
            Intent intent = new Intent(this, MeditationSetupActivity.class);
            intent.putExtra(EXTRA_SUGGESTED_MINUTES, i.suggestedMoreMinutesToday);
            startActivity(intent);
            return;
        }

        if (i.suggestionType == MeditationInsightsRepository.SUGGESTION_RAISE_GOAL) {
            int nextGoal = i.suggestedNewGoalMinutes; // <--- Feldname wie von dir ergänzt
            if (nextGoal > 0) {
                showRaiseGoalDialog(nextGoal);
            }
            return;
        }


        if (i.suggestionType == MeditationInsightsRepository.SUGGESTION_ADAPT_GOAL) {
            int current = i.goalMinutesPerDay;
            int suggested = clampGoal(current / 2);
            showLowerGoalDialog(current, suggested);
            return;
        }
    }
    private int clampGoal(int v) {
        if (v < AppSettings.MIN_DAILY_GOAL_MINUTES) return AppSettings.MIN_DAILY_GOAL_MINUTES;
        if (v > AppSettings.MAX_DAILY_GOAL_MINUTES) return AppSettings.MAX_DAILY_GOAL_MINUTES;
        return v;
    }
    private void showLowerGoalDialog(int currentGoal, int suggestedGoal) {
        new AlertDialog.Builder(this)
                .setTitle("Ziel anpassen?")
                .setMessage("Aktuell: " + currentGoal + "\nVorschlag: " + suggestedGoal)
                .setPositiveButton("Übernehmen", (d, which) -> {
                    AppSettings.setDailyGoalMinutes(this, suggestedGoal);
                    Toast.makeText(this, "Neues Ziel: " + suggestedGoal, Toast.LENGTH_SHORT).show();
                    MeditationInsightsRepository.getInstance(this).recompute();
                })
                .setNegativeButton("Später", null)
                .show();
    }

    private void showRaiseGoalDialog(int nextGoal) {
        new AlertDialog.Builder(this)
                .setTitle("Ziel anheben?")
                .setMessage("Neues Tagesziel: " + nextGoal)
                .setPositiveButton("Übernehmen", (d, which) -> {
                    AppSettings.setDailyGoalMinutes(this, nextGoal);
                    Toast.makeText(this, "Neues Ziel: " + nextGoal, Toast.LENGTH_SHORT).show();

                    // Optional: sofort neu rendern. Wenn dein Repo auch Pref-Listener hat, reicht das ggf. allein.
                    MeditationInsightsRepository.getInstance(this).recompute();
                })
                .setNegativeButton("Später", null)
                .show();
    }

}
