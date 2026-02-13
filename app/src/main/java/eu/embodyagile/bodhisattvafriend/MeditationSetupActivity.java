package eu.embodyagile.bodhisattvafriend;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.history.MeditationInsights;
import eu.embodyagile.bodhisattvafriend.history.MeditationInsightsRepository;

import eu.embodyagile.bodhisattvafriend.model.Practice;
import eu.embodyagile.bodhisattvafriend.settings.AppSettings;

public class MeditationSetupActivity extends BaseActivity {

    public static final String EXTRA_PRACTICE_ID = "extra_practice_id";
    private static final String TAG = "MEDI_SETUP";
    private static final String EXTRA_SUGGESTED_MINUTES = "extra_suggested_minutes";

    // --- reuse existing constants from MeditationActivity ---
    // NOTE: keep using MeditationActivity extras to avoid divergence.
    // Setup receives: EXTRA_PRACTICE_ID, EXTRA_SUGGESTED_MINUTES (optional)
    // Setup sends:   EXTRA_PRACTICE_ID, EXTRA_SELECTED_MINUTES, EXTRA_PLAY_AUDIO, EXTRA_FROM_POMODORO=false

    // suggestion / insights
    private MeditationInsightsRepository insightsRepo;
    private MeditationInsights insights;
    private int suggestedMinutes = -1;
    private long lastDayIndex = -1;

    // practice
    private Practice practice;

    // UI containers (existing XML)
    private View setupContainer;


    // setup UI
    private View manualLayout;
    private View suggestionLayout;
    private GridLayout durationButtonsLayout;
    private TextView tvSuggestionText;

    private ImageButton more_button;
    private ImageButton less_button;
    private TextView text_timer;
    private Button startButton;

    private boolean showSuggestionMode = false;
    private boolean userTouchedDuration = false;

    // state
    private int selectedDurationMinutes = 0;
    private boolean playAudio = false;

    private final MeditationInsightsRepository.Listener insightsListener = updated -> {
        insights = updated;
        runOnUiThread(() -> {
            if (setupContainer != null && setupContainer.getVisibility() == VISIBLE) {
                insights = updated;
                bindTodayInsightToSuggestion();
                        }
        });
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_meditation_setup);
        setupFooter(FooterTab.PRACTICE);

        // IMPORTANT: only show SETUP part of existing layout
        setupContainer = findViewById(R.id.layout_setup);


        if (setupContainer != null) setupContainer.setVisibility(VISIBLE);


        // suggested minutes may come from History/Insights path
        suggestedMinutes = getIntent().getIntExtra(MeditationSetupActivity.EXTRA_SUGGESTED_MINUTES, -1);

        // Lautstärke-Tasten steuern Klangschale (keine inhaltliche Änderung)
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // repo + practice load
        PracticeRepository repository = PracticeRepository.getInstance();
        repository.init(this);

        String practiceId = getIntent().getStringExtra(MeditationSetupActivity.EXTRA_PRACTICE_ID);
        practice = repository.getPracticeById(practiceId);
        if (practice == null) practice = repository.getFallbackPractice();

        // name view exists in your layout; harmless to set here
        TextView nameView = findViewById(R.id.text_practice_name);
        if (nameView != null && practice != null) nameView.setText(practice.getName());

        Log.d(TAG, "onCreate: practiceId=" + practiceId + " suggestedMinutes(extra)=" + suggestedMinutes
                + " practice=" + (practice == null ? "null" : practice.getId()));

        initSetupView();

        lastDayIndex = currentDayIndex();

        if (practice != null) {
            setupPracticeUI();
            refreshInsightsIfNeeded(); // <-- einmal initial sauber berechnen + binden
        } else {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (insightsRepo == null) insightsRepo = MeditationInsightsRepository.getInstance(this);
        insightsRepo.addListener(insightsListener);
    }

    @Override
    protected void onStop() {
        if (insightsRepo != null) insightsRepo.removeListener(insightsListener);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshInsightsIfNeeded();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    // region Setup View Methods (copied with minimal edits)

    private void initSetupView() {
        if (setupContainer == null) return;

        manualLayout = setupContainer.findViewById(R.id.layout_manual);
        suggestionLayout = setupContainer.findViewById(R.id.layout_suggestion);

        durationButtonsLayout = setupContainer.findViewById(R.id.layout_duration_buttons);
        tvSuggestionText = setupContainer.findViewById(R.id.tv_suggestion_text);

        text_timer = setupContainer.findViewById(R.id.text_timer);
        startButton = setupContainer.findViewById(R.id.button_start_meditation);





        less_button = setupContainer.findViewById(R.id.button_less);
        more_button = setupContainer.findViewById(R.id.button_more);

        View audioOptionLayout = setupContainer.findViewById(R.id.layout_audio_option);
        androidx.appcompat.widget.SwitchCompat audioSwitch = setupContainer.findViewById(R.id.switch_audio);

        if (audioSwitch != null) {
            audioSwitch.setOnCheckedChangeListener((button, isChecked) -> playAudio = isChecked);
        }


        if (less_button != null) {
            less_button.setOnClickListener(view -> {
                getTimeForMeditation();
                if (selectedDurationMinutes > 1) {
                    selectedDurationMinutes--;
                    if (text_timer != null) text_timer.setText(String.valueOf(selectedDurationMinutes));
                }
            });
        }

        if (more_button != null) {
            more_button.setOnClickListener(view -> {
                getTimeForMeditation();
                if (selectedDurationMinutes < 99) {
                    selectedDurationMinutes++;
                    if (text_timer != null) text_timer.setText(String.valueOf(selectedDurationMinutes));
                }
            });
        }



        if (audioOptionLayout != null) {
            if (practice != null && practice.getAudioConfig() != null) {
                audioOptionLayout.setVisibility(View.VISIBLE);
            } else {
                audioOptionLayout.setVisibility(View.GONE);
            }
        }

        showSuggestionMode = AppSettings.isMeditationSetupSuggestionModeDefault(this);

        Button toggleManual = setupContainer.findViewById(R.id.button_duration_toggle);
        Button toggleSuggestion = setupContainer.findViewById(R.id.button_suggestion_toggle);

        View.OnClickListener toggleListener = v -> {
            showSuggestionMode = !showSuggestionMode;
            applyDurationChooserMode();
        };
        if (toggleManual != null) toggleManual.setOnClickListener(toggleListener);
        if (toggleSuggestion != null) toggleSuggestion.setOnClickListener(toggleListener);
    }

    private void setupPracticeUI() {
        if (practice == null) return;

        // Standard-Dauern aus der Praxis
        List<Integer> defaults = practice.getDefaultDurationsMinutes();
        int maxButtons = 5;

        if (durationButtonsLayout != null) durationButtonsLayout.removeAllViews();

        for (int i = 0; defaults != null && i < defaults.size() && i < maxButtons; i++) {
            final int minutes = defaults.get(i);

            Button b = new Button(this);
            b.setText(minutes + " " + getString(R.string.minutes_abbreviated));
            b.setAllCaps(false);
            b.setBackgroundResource(R.drawable.quickcheck_button_default);

            b.setOnClickListener(v -> {
                selectedDurationMinutes = minutes;
                if (text_timer != null) text_timer.setText(String.valueOf(minutes));
            });

            if (durationButtonsLayout != null) durationButtonsLayout.addView(b);
        }

        int last = loadLastDuration(practice.getId());
        if (last > 0) {
            selectedDurationMinutes = last;
        } else if (defaults != null && !defaults.isEmpty()) {
            selectedDurationMinutes = defaults.get(0);
        } else {
            selectedDurationMinutes = 1;
        }
        if (text_timer != null) text_timer.setText(String.valueOf(selectedDurationMinutes));

        // suggested minutes override (from History / Insights)
        if (suggestedMinutes > 0) {
            selectedDurationMinutes = suggestedMinutes;
            if (text_timer != null) text_timer.setText(String.valueOf(suggestedMinutes));
        }

        Log.d(TAG, "setupPracticeUI: selectedDurationMinutes=" + selectedDurationMinutes
                + " suggestedMinutes=" + suggestedMinutes);

        insightsRepo = MeditationInsightsRepository.getInstance(this);
        insights = insightsRepo.getCached();

        bindTodayInsightToSuggestion();

        if (text_timer != null) {
            text_timer.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String text = text_timer.getText().toString().trim();
                    try {
                        int customMinutes = Integer.parseInt(text);
                        if (customMinutes > 0) selectedDurationMinutes = customMinutes;
                    } catch (NumberFormatException ignored) {
                    }
                }
            });
        }

        if (startButton != null) {
            startButton.setOnClickListener(v -> onStartClicked());
        }
    }

    private void onStartClicked() {
        getTimeForMeditation();
        if (practice != null) saveLastDuration(practice.getId(), selectedDurationMinutes);

        Intent i = new Intent(this, MeditationActivity.class);
        if (practice != null) i.putExtra(MeditationActivity.EXTRA_PRACTICE_ID, practice.getId());
        i.putExtra(MeditationActivity.EXTRA_SELECTED_MINUTES, selectedDurationMinutes);
        i.putExtra(MeditationActivity.EXTRA_FROM_POMODORO, false);
        i.putExtra(MeditationActivity.EXTRA_PLAY_AUDIO, playAudio);
        startActivity(i);
    }

    private int getTimeForMeditation() {
        if (text_timer == null) return selectedDurationMinutes;

        String customText = text_timer.getText().toString().trim();
        if (!customText.isEmpty()) {
            try {
                int customMinutes = Integer.parseInt(customText);
                selectedDurationMinutes = Math.max(1, customMinutes);
            } catch (NumberFormatException ignored) {
                selectedDurationMinutes = 1;
            }
        } else {
            selectedDurationMinutes = 1;
        }
        return selectedDurationMinutes;
    }

    private void bindTodayInsightToSuggestion() {
        if (tvSuggestionText == null) return;

        String headline1 = "";
        String suggestion = "";

        if (insights != null) {
            headline1 = safe(insights.headline1);
            suggestion = safe(insights.suggestionText);
            suggestedMinutes = insights.suggestedMoreMinutesToday;
        }

        if (suggestion.isEmpty()) suggestion = getString(R.string.history_not_yet);

        StringBuilder sb = new StringBuilder();
        if (!headline1.isEmpty()) sb.append(headline1);
        if (sb.length() > 0) sb.append(" ");
        sb.append(suggestion);

        tvSuggestionText.setText(sb.toString());

        boolean canApplyMinutes = (suggestedMinutes > 0);
        tvSuggestionText.setClickable(canApplyMinutes);
        tvSuggestionText.setOnClickListener(v -> {
            if (!canApplyMinutes) return;
            userTouchedDuration = true;
            selectedDurationMinutes = suggestedMinutes;
            if (text_timer != null) text_timer.setText(String.valueOf(suggestedMinutes));
        });

        applyDurationChooserMode();
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private void applyDurationChooserMode() {
        if (manualLayout == null || suggestionLayout == null) return;

        boolean hasSuggestion = suggestedMinutes > 0;
        if (!hasSuggestion) showSuggestionMode = false;

        manualLayout.setVisibility(showSuggestionMode ? GONE : VISIBLE);
        suggestionLayout.setVisibility(showSuggestionMode ? VISIBLE : GONE);
    }

    private int loadLastDuration(String practiceId) {
        return AppSettings.getLastDurationMinutes(this, practiceId, 0);
    }

    private void saveLastDuration(String practiceId, int minutes) {
        AppSettings.setLastDurationMinutes(this, practiceId, minutes);
    }

    private void refreshInsightsIfNeeded() {
        if (insightsRepo == null) insightsRepo = MeditationInsightsRepository.getInstance(this);

        long today = currentDayIndex();
        boolean dayChanged = (lastDayIndex != -1 && today != lastDayIndex);
        lastDayIndex = today;

        // Always refresh when visible; recompute is cheap enough
        if (setupContainer != null && setupContainer.getVisibility() == VISIBLE) {
            insightsRepo.recompute();
            insights = insightsRepo.getCached();
            bindTodayInsightToSuggestion();
        } else if (dayChanged) {
            insightsRepo.recompute();
            insights = insightsRepo.getCached();
            bindTodayInsightToSuggestion();
        }
    }

    private long currentDayIndex() {
        java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getDefault());
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        return start / (24L * 60L * 60L * 1000L);
    }

    // endregion
}
