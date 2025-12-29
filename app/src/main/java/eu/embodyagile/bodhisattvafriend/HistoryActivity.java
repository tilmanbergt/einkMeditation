package eu.embodyagile.bodhisattvafriend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import eu.embodyagile.bodhisattvafriend.history.MeditationInsights;
import eu.embodyagile.bodhisattvafriend.history.MeditationInsightsRepository;

public class HistoryActivity extends BaseActivity {

    // TODO step 2: implement in MeditationActivity
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
    private Button closeButton;

    private MeditationInsightsRepository repo;
    private final MeditationInsightsRepository.Listener listener = this::render;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

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

        btnShowList = findViewById(R.id.button_show_last_practices);
        closeButton = findViewById(R.id.button_history_close);

        repo = MeditationInsightsRepository.getInstance(this);

        btnShowList.setOnClickListener(v -> {
            Intent intent = new Intent(this, SessionListActivity.class);
            startActivity(intent);
        });

        closeButton.setOnClickListener(v -> finish());

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
        if (i.suggestionText != null && !i.suggestionText.trim().isEmpty()) {
            tvSuggestion.setText(i.suggestionText);
            tvSuggestion.setVisibility(View.VISIBLE);
        } else {
            tvSuggestion.setText("");
            tvSuggestion.setVisibility(View.GONE);
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
            Intent intent = new Intent(this, MeditationActivity.class);
            intent.putExtra(EXTRA_SUGGESTED_MINUTES, i.suggestedMoreMinutesToday);
            startActivity(intent);
            return;
        }

        if (i.suggestionType == MeditationInsightsRepository.SUGGESTION_ADAPT_GOAL) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
    }
}
