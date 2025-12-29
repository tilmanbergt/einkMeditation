package eu.embodyagile.bodhisattvafriend;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.NotificationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.helper.FeedbackHelper;
import eu.embodyagile.bodhisattvafriend.history.SessionLogEntry;
import eu.embodyagile.bodhisattvafriend.history.SessionLogManager;
import eu.embodyagile.bodhisattvafriend.logic.InnerCondition;
import eu.embodyagile.bodhisattvafriend.logic.TimeAvailable;
import eu.embodyagile.bodhisattvafriend.model.Practice;
import eu.embodyagile.bodhisattvafriend.settings.AppSettings;

public class MeditationActivity extends BaseActivity {
    private boolean useDndDuringSession = true; // your flag
    private NotificationManager nm;
    private int prevFilter = -1;
    //region general fields
    private boolean playAudio = false;
    private FeedbackHelper feedbackHelper;

    public static final String EXTRA_PRACTICE_ID = "extra_practice_id";

    public static final String EXTRA_PRE_TIME = "extra_pre_time";
    public static final String EXTRA_PRE_INNER = "extra_pre_inner";
    public static final String EXTRA_SUGGESTED_MINUTES = "extra_suggested_minutes";
    private int suggestedMinutes = -1;

    private Practice practice;
    private MediaPlayer audioPlayer;
    private boolean isAudioPrepared = false;
   // private InnerCondition preInnerCondition;
    //private TimeAvailable preTime;
    private boolean ignoreOvertime = false;
    private View audioOptionLayout;

    private enum Mode {
        SETUP,
        RUN,
        SUMMARY
    }

    private TextView nameView;
    private View setupContainer;
    private View runContainer;
    private View summaryContainer;


//endregion

//region Setup View Fields
    // Setup-Views
private GridLayout durationButtonsLayout;
    private ImageButton more_button;
    private ImageButton less_button;

    private Button durationButton1;
    private Button durationButton2;
    //private EditText customDurationEdit;
    private Button startButton;
    //private Button homeButton;
    private TextView text_timer;
    private int selectedDurationMinutes = 0;


    int plannedMinutes;
    long plannedMillis;
    //endregion


    // region Run -View fields
    private boolean inPreTimer = false;
    // Fields
    private TextView instructionView;
    private TextView timerView;
    private Button pauseContinueButton;
    private Button endButton;
    private Button cancelButton;

    //Logic

    private CountDownTimer timer;
    private CountDownTimer preMeditationTimer;
    private long remainingMillis = 0L;
    private boolean isRunning = false;
    private boolean isPaused = false;

    private MediaPlayer bellPlayer;

//endregion

    //region Summary Views Fields
    private Button btnSave;
    // Feature flag (disable candle functionality by setting false)
    private boolean featureCandleTimer = true;

    // Views
    private android.widget.ImageView candleView;

    // State
    private boolean persistentTimeMode = false; // set by double-tap on candle
    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    // Timing
    private static final long FIRST_10S_MS = 10_000L;
    private static final long TEMP_SHOW_MS = 5_000L;


    // private TextView practiceNameView;
    private TextView durationsView;
    private TextView tvIgnoreOvertime;



    // Logic
    long baseActualMillis;
    private InnerCondition innerAfter = null;
    //  private boolean goToVowAfter = false;

    private boolean allowOvertime;
    private long finishTimestamp;

    private long currentOvertimeMillis = 0L;
    private CountDownTimer overtimeTimer;
    private boolean vibrationOnStartEnd;
    private boolean flashOnStartEnd;
    private int preMeditationCountdownSeconds;

// endregion

    // region Create and Destroy Methods


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_meditation);
        feedbackHelper = new FeedbackHelper(this);
        suggestedMinutes = getIntent().getIntExtra(EXTRA_SUGGESTED_MINUTES, -1);

       reloadPracticeSettings();
        // Lautstärke-Tasten steuern Klangschale
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        PracticeRepository repository = PracticeRepository.getInstance();
        repository.init(this);   // <-- WICHTIG, jedes Mal aufrufen für sprachewechsel

        String practiceId = getIntent().getStringExtra(EXTRA_PRACTICE_ID);
        practice = repository.getPracticeById(practiceId);
        if (practice == null) {
            practice = repository.getFallbackPractice();
        }





        nameView = findViewById(R.id.text_practice_name);
        Log.d("meditation", "onCreate: practice field found" + nameView);
        Log.d("meditation", "onCreate: practice " + practice);

        nameView.setText(practice.getName());
        // Layout-Container
        setupContainer = findViewById(R.id.layout_setup);
        runContainer = findViewById(R.id.layout_session);
        summaryContainer = findViewById(R.id.layout_summary);

        // initial nur Setup zeigen
        switchMode(Mode.SETUP);

        // Views initialisieren
        initSetupView();
        initRunView();
        initSummaryView();

        if (practice != null) {
            setupPracticeUI();
        } else {
            // Falls etwas schiefgeht, einfach zurück
            finish();
        }
    }
    private boolean hasDndAccess() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || (nm != null && nm.isNotificationPolicyAccessGranted());
    }

    private void openDndAccessSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
    }
    private void applyDndIfEnabled() {
        if (!useDndDuringSession) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        if (nm == null) return;

        if (!nm.isNotificationPolicyAccessGranted()) return;

        if (prevFilter == -1) prevFilter = nm.getCurrentInterruptionFilter();

        // “Fully” quiet:
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
    }

    private void restoreDndIfChanged() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        if (nm == null) return;

        if (prevFilter != -1 && nm.isNotificationPolicyAccessGranted()) {
            nm.setInterruptionFilter(prevFilter);
        }
        prevFilter = -1;
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        endSessionCleanup();   // <-- ADD THIS
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (preMeditationTimer != null) {
            preMeditationTimer.cancel();
            preMeditationTimer = null;
        }
        if (bellPlayer != null) {
            bellPlayer.release();
            bellPlayer = null;
        }
        uiHandler.removeCallbacksAndMessages(null);

        stopPracticeAudio();

    }

    //endregion


    // region Setup View Methods
    private void initSetupView() {
        if (setupContainer == null) return;


        durationButtonsLayout = setupContainer.findViewById(R.id.layout_duration_buttons);

         text_timer = setupContainer.findViewById(R.id.text_timer);
        startButton = setupContainer.findViewById(R.id.button_start_meditation);
        ImageButton settingsButton = setupContainer.findViewById(R.id.button_settings);
        ImageButton otherPractice = setupContainer.findViewById((R.id.button_andere_praxis));
        boolean enabled = setupContainer.getResources().getBoolean(R.bool.config_feature_change_practice);
        otherPractice.setVisibility(enabled ? View.VISIBLE : View.GONE);


        ImageButton historyButton = setupContainer.findViewById(R.id.button_history);

        less_button = setupContainer.findViewById(R.id.button_less);
        more_button = setupContainer.findViewById(R.id.button_more);

        audioOptionLayout = setupContainer.findViewById(R.id.layout_audio_option);
        androidx.appcompat.widget.SwitchCompat audioSwitch = setupContainer.findViewById(R.id.switch_audio);

        audioSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            playAudio = isChecked;
        });
 historyButton.setOnClickListener(view -> {
            Intent intent = new Intent(MeditationActivity.this, HistoryActivity.class);
            startActivity(intent); });
        settingsButton.setOnClickListener(view -> {
            Intent intent = new Intent(MeditationActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

less_button.setOnClickListener(view -> {
    getTimeForMeditation();

    if (selectedDurationMinutes > 1) {
        selectedDurationMinutes--;
        text_timer.setText(String.valueOf(selectedDurationMinutes));
    }
});
more_button.setOnClickListener(view -> {
    getTimeForMeditation();

    if (selectedDurationMinutes < 99) {
        selectedDurationMinutes++;
        text_timer.setText(String.valueOf(selectedDurationMinutes));
    }
});


        otherPractice.setOnClickListener(view -> {
            Intent intent = new Intent(MeditationActivity.this, MeditationSelectionActivity.class);
            intent.putExtra(MeditationActivity.EXTRA_PRACTICE_ID, practice.getId());

            startActivity(intent);
        });

        if (practice.getAudioConfig() != null) {
            audioOptionLayout.setVisibility(View.VISIBLE);
        } else {
            audioOptionLayout.setVisibility(View.GONE);
        }


    }

    private void setupPracticeUI() {
        // Setup-Screen sichtbar, Session-Screen ausblenden
        switchMode(Mode.SETUP);

      //  homeButton.setVisibility(VISIBLE);


        instructionView.setText(practice.getInstructionText());

        // Standard-Dauern aus der Praxis
        List<Integer> defaults = practice.getDefaultDurationsMinutes();
        int maxButtons = 5;

// rebuild buttons
        durationButtonsLayout.removeAllViews();

        for (int i = 0; i < defaults.size() && i < maxButtons; i++) {
            final int minutes = defaults.get(i);

            Button b = new Button(this);
            b.setText(minutes + " " + getString(R.string.minutes_abbreviated));
            b.setAllCaps(false);

            // Optional: reuse your style (pick the one your old duration buttons used)
            b.setBackgroundResource(R.drawable.quickcheck_button_default);

            b.setOnClickListener(v -> {
                if (!isRunning && !isPaused) {
                    selectedDurationMinutes = minutes;
                    text_timer.setText(String.valueOf(minutes));
                }
            });

            durationButtonsLayout.addView(b);
        }
        int last = loadLastDuration(practice.getId());

        if (last > 0) {
            selectedDurationMinutes = last;
            text_timer.setText(String.valueOf(last));
        } else {
            // fallback: use first default duration if available (instead of hardcoded 1)
            if (defaults != null && !defaults.isEmpty()) {
                selectedDurationMinutes = defaults.get(0);
                text_timer.setText(String.valueOf(selectedDurationMinutes));
            } else {
                selectedDurationMinutes = 1;
                text_timer.setText("1");
            }
        }
// --- suggested minutes override (from History / Insights) ---
        if (suggestedMinutes > 0 && !isRunning && !isPaused) {
            selectedDurationMinutes = suggestedMinutes;
            text_timer.setText(String.valueOf(suggestedMinutes));
        }


        // Custom-Dauer beim Verlassen des Feldes übernehmen (Sicherheitsnetz)
        text_timer.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !isRunning && !isPaused) {
                String text = text_timer.getText().toString().trim();
                try {
                    int customMinutes = Integer.parseInt(text);
                    if (customMinutes > 0) {
                        selectedDurationMinutes = customMinutes;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        });

        startButton.setOnClickListener(v -> onStartClicked());
        //homeButton.setOnClickListener(v -> onHomeClicked());

        pauseContinueButton.setOnClickListener(v -> onPauseContinueClicked());
        endButton.setOnClickListener(v -> onEndClicked());
        cancelButton.setOnClickListener(v -> onCancelClicked());
    }

    private void onStartClicked() {
        reloadPracticeSettings(); // <- ensures latest value even if something weird happens

        // Custom-Dauer bevorzugen, wenn etwas eingetragen ist
        getTimeForMeditation();


        saveLastDuration(practice.getId(), selectedDurationMinutes);
        remainingMillis = selectedDurationMinutes * 60_000L;

        // Wechsle in den Session-Screen
        switchMode(Mode.RUN);

        //homeButton.setVisibility(GONE);

        if (preMeditationCountdownSeconds > 0) {
            startPreMeditationCountdown();
        } else {
            startMeditation();
        }
    }

    private int getTimeForMeditation() {
        String customText = text_timer.getText().toString().trim();
        if (!customText.isEmpty()) {
            try {
                int customMinutes = Integer.parseInt(customText);
                if (customMinutes > 0) {
                    selectedDurationMinutes = customMinutes;
                } else {
                    selectedDurationMinutes = 1;
                }
            } catch (NumberFormatException ignored) {
                selectedDurationMinutes = 1;
            }
        } else {
            selectedDurationMinutes = 1;
        }
        return selectedDurationMinutes;
    }

    private void onHomeClicked() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (preMeditationTimer != null) {
            preMeditationTimer.cancel();
            preMeditationTimer = null;
        }
        if (bellPlayer != null) {
            bellPlayer.release();
            bellPlayer = null;
        }
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void onCancelClicked() {
        if (preMeditationTimer != null) {
            preMeditationTimer.cancel();
            preMeditationTimer = null;
        }
        endSessionCleanup();   // <-- ADD THIS

        switchMode(Mode.SETUP);
    }

    private int loadLastDuration(String practiceId) {
        int last = AppSettings.getLastDurationMinutes(this, practiceId, 0);

return last;

    }

    private void saveLastDuration(String practiceId, int minutes) {
        AppSettings.setLastDurationMinutes(this, practiceId, minutes);

    }

    private void updateDurationsText() {
        if (durationsView == null) return;
        long baseSeconds = baseActualMillis > 0 ? Math.round(baseActualMillis / 1000f) : 0;

        long baseMinutes = baseSeconds > 0 ? Math.round(baseSeconds / 60f) : 0;
        long baseSeconds2 = baseSeconds > 0 ? baseSeconds % 60 : 0;

        StringBuilder sb = new StringBuilder();

        sb.append(baseMinutes).append(":").append(String.format("%02d",baseSeconds2));

        if (allowOvertime & !ignoreOvertime) {
            long totalSeconds = currentOvertimeMillis / 1000L;
            long om = totalSeconds / 60;
            long os = totalSeconds % 60;
            if (totalSeconds > 5) {
                sb.append(" +")
                        .append(String.format("%1d:%02d", om, os)).append("");
                tvIgnoreOvertime.setVisibility(VISIBLE);
            }

        }

        durationsView.setText(sb.toString());
    }
    // endregion


    // region Run View
    private void initRunView() {
        if (runContainer == null) return;
        instructionView = runContainer.findViewById(R.id.text_practice_instruction);
        timerView = runContainer.findViewById(R.id.text_timer);
        pauseContinueButton = runContainer.findViewById(R.id.button_pause_continue);
        endButton = runContainer.findViewById(R.id.button_end);
        cancelButton = runContainer.findViewById(R.id.button_cancel);
        candleView = runContainer.findViewById(R.id.image_candle);

        if (!featureCandleTimer) {
            if (candleView != null) candleView.setVisibility(View.GONE);
            if (timerView != null) timerView.setVisibility(View.VISIBLE);
            return;
        }

        // Ensure touch works even if something else changes later
        candleView.setClickable(true);
        timerView.setClickable(true);

        final GestureDetector candleDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent e) { return true; }

                    @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                        // DEBUG:
                        // Log.d("CANDLE", "single tap candle");
                        showTimeTemporarily();
                        return true;
                    }
                    @Override public boolean onDoubleTap(MotionEvent e) {
                        // DEBUG:
                        // Log.d("CANDLE", "double tap candle");
                        persistentTimeMode = true;
                        setRunDisplayTime();
                        return true;
                    }
                });

        candleView.setOnTouchListener((v, ev) -> {
            candleDetector.onTouchEvent(ev);
            if (ev.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return true; // <-- IMPORTANT: keep receiving events
        });

        final GestureDetector timeDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent e) { return true; }

                    @Override public boolean onDoubleTap(MotionEvent e) {
                        // DEBUG:
                        // Log.d("CANDLE", "double tap time");
                        persistentTimeMode = false;
                        uiHandler.removeCallbacks(revertTempTimeToCandle);
                        setRunDisplayCandle();
                        return true;
                    }
                });

        timerView.setOnTouchListener((v, ev) -> {
            timeDetector.onTouchEvent(ev);
            if (ev.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return true; // <-- IMPORTANT
        });
    }
    // Runnables
    private final Runnable showCandleAfterFirst10s = () -> {
        if (!featureCandleTimer) return;
        if (!persistentTimeMode) {
            setRunDisplayCandle();
        }
    };
    private void endSessionCleanup() {
        // stop DND (this is the important one)
        restoreDndIfChanged();

        // optional but good hygiene for your other session features
        uiHandler.removeCallbacks(showCandleAfterFirst10s);
        uiHandler.removeCallbacks(revertTempTimeToCandle);
        inPreTimer = false;
    }
    private final Runnable revertTempTimeToCandle = () -> {
        if (!featureCandleTimer) return;
        if (!persistentTimeMode) {
            setRunDisplayCandle();
        }
    };


    private void setRunDisplayTime() {
        if (timerView != null) timerView.setVisibility(View.VISIBLE);
        if (candleView != null) candleView.setVisibility(View.GONE);
    }

    private void setRunDisplayCandle() {
        if (timerView != null) timerView.setVisibility(View.GONE);
        if (candleView != null) candleView.setVisibility(View.VISIBLE);
    }

    private void showTimeTemporarily() {
        if (!featureCandleTimer) return;

        // If user already chose persistent time, do nothing special.
        if (persistentTimeMode) {
            setRunDisplayTime();
            return;
        }

        setRunDisplayTime();
        uiHandler.removeCallbacks(revertTempTimeToCandle);
        uiHandler.postDelayed(revertTempTimeToCandle, TEMP_SHOW_MS);
    }



    private void onPauseContinueClicked() {
        if (isRunning && !isPaused) {
            // Pause
            pauseTimer();
            if (playAudio && audioPlayer.getCurrentPosition() < audioPlayer.getDuration()) {
                audioPlayer.pause();
            }
        } else if (!isRunning && isPaused) {
            // Weiter
            startTimer(false); // without sound
            if (playAudio && audioPlayer.getCurrentPosition() < audioPlayer.getDuration()) {
                audioPlayer.start();
            }
        }
    }

    private void startPracticeAudio() {
        Practice.AudioConfig audio = practice.getAudioConfig();
        if (audio == null) return;

        int resId = PracticeRepository.getInstance().resolveAudioResId(audio);
        if (resId == 0) return;

        releaseAudioPlayer(); // safety

        audioPlayer = MediaPlayer.create(this, resId);
        if (audioPlayer == null) return;

        // Looping logic
        boolean shouldLoop = audio.isLoop();
        audioPlayer.setLooping(shouldLoop);

        // Optional: lower volume if you want it subtle
        // audioPlayer.setVolume(0.6f, 0.6f);

        audioPlayer.setOnPreparedListener(mp -> {
            isAudioPrepared = true;
            mp.start();
        });

        // MediaPlayer.create(...) is usually already prepared,
        // but OnPreparedListener doesn’t hurt.
        isAudioPrepared = true;
        audioPlayer.start();
    }

    private void stopPracticeAudio() {
        if (audioPlayer != null) {
            if (audioPlayer.isPlaying()) {
                audioPlayer.stop();
            }
            audioPlayer.reset();
            audioPlayer.release();
            audioPlayer = null;
            isAudioPrepared = false;
        }
    }

    private void releaseAudioPlayer() {
        if (audioPlayer != null) {
            try {
                audioPlayer.release();
            } catch (Exception ignored) {
            }
            audioPlayer = null;
            isAudioPrepared = false;
        }
    }

    private void onEndClicked() {
        // nur reagieren, wenn jemals gestartet wurde
        if (!isRunning && !isPaused) {
            return;
        }

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        isRunning = false;
        isPaused = false;
        finishTimestamp = System.currentTimeMillis();

        timerView.setText("Fertig");
        endSessionCleanup();   // <-- ADD THIS (missing today) :contentReference[oaicite:3]{index=3}

        signalStartEnd(); // Klang beim vorzeitigen Beenden
        allowOvertime = false;
        releaseAudioPlayer();
        openSummaryScreen();
    }

    private void startPreMeditationCountdown() {
        endButton.setVisibility(GONE);
        cancelButton.setVisibility(VISIBLE);
        pauseContinueButton.setVisibility(GONE);



        if (featureCandleTimer) {
            inPreTimer = true;

            // Always show the time during pretimer
            persistentTimeMode = false;
            uiHandler.removeCallbacks(showCandleAfterFirst10s);
            uiHandler.removeCallbacks(revertTempTimeToCandle);
            setRunDisplayTime();
        }
        preMeditationTimer = new CountDownTimer(preMeditationCountdownSeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000L;
                timerView.setText("-" + String.format("%02d", seconds));
            }

            @Override
            public void onFinish() {
                if (featureCandleTimer) inPreTimer = false;
                startMeditation();
            }
        };
        preMeditationTimer.start();
    }
    private void updateDurationUi(int minutes) {
        selectedDurationMinutes = minutes;
        durationsView.setText(minutes); // your actual view / formatting
    }
    @Override
    protected void onStop() {
        restoreDndIfChanged();
        super.onStop();
    }
    private void startMeditation() {
        updateTimerTextFromMillis();



        pauseContinueButton.setText(getString(R.string.pause));
        endButton.setVisibility(VISIBLE);
        cancelButton.setVisibility(GONE);
        pauseContinueButton.setVisibility(VISIBLE);

        if (playAudio) {
            startPracticeAudio();
        }
        if (featureCandleTimer) {
            persistentTimeMode = false;              // default: candle after 10s
            setRunDisplayTime();                     // first 10 seconds always time
            uiHandler.removeCallbacks(showCandleAfterFirst10s);
            uiHandler.postDelayed(showCandleAfterFirst10s, FIRST_10S_MS);
        } else {
            setRunDisplayTime();
        }

        startTimer(true); // mit Klang am Anfang
    }

    private void startTimer(boolean signalStart) {
        if (remainingMillis <= 0L) {
            remainingMillis = selectedDurationMinutes * 60_000L;
        }

        if (timer != null) {
            timer.cancel();
        }

        if (signalStart) {
            signalStartEnd();
        }

        if (useDndDuringSession) {
            if (!hasDndAccess()) {
                openDndAccessSettings();
                // optionally show a Toast: “Enable DND access to silence calls”
            } else {
                applyDndIfEnabled();
            }
        }

        isRunning = true;
        isPaused = false;
        pauseContinueButton.setText(getString(R.string.pause));

        timer = new CountDownTimer(remainingMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                updateTimerTextFromMillis();
            }

            @Override
            public void onFinish() {
                isRunning = false;
                isPaused = false;
                remainingMillis = 0L;
                finishTimestamp = System.currentTimeMillis();

                timerView.setText(getString(R.string.fertig));
                endSessionCleanup();   // <-- ADD THIS
                signalStartEnd(); // Klang am natürlichen Ende

                allowOvertime = true;
                openSummaryScreen();
            }
        };
        timer.start();
    }

    private void pauseTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        isRunning = false;
        isPaused = true;
        pauseContinueButton.setText(getString(R.string.weiter));
    }

    private void updateTimerTextFromMillis() {
        long displayMillis = remainingMillis;
        if (displayMillis <= 0L) {
            displayMillis = selectedDurationMinutes * 60_000L;
        }

        long totalSeconds = displayMillis / 1000L;
        long m = totalSeconds / 60;
        long s = totalSeconds % 60;
        String text = String.format("%02d:%02d", m, s);
        timerView.setText(text);
    }

    private void signalStartEnd() {
        try {
            if (bellPlayer == null) {
                bellPlayer = MediaPlayer.create(this, R.raw.klangschale);
                bellPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            if (bellPlayer != null) {
                bellPlayer.seekTo(0);
                bellPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // NEW: feedback at early end
        if (vibrationOnStartEnd) {
            feedbackHelper.vibrateShort();
        }
        if (flashOnStartEnd) {
            feedbackHelper.flashBacklight(this, 500);
        }
    }

    // endregion


    // region Summary View Methods
    private void openSummaryScreen() {
        endSessionCleanup();   // <-- ADD THIS
        // 1) Modus umschalten
        switchMode(Mode.SUMMARY);

        // 2) UI mit aktuellen Session-Daten füllen


        plannedMinutes = selectedDurationMinutes > 0 ? selectedDurationMinutes : 5;
        plannedMillis = plannedMinutes * 60_000L;


        if (allowOvertime) {
            // natürliches Ende: Basis = geplante Dauer
            baseActualMillis = plannedMillis;
        } else {
            // frühzeitig beendet: Basis = tatsächlich gesessene Zeit
            long elapsed = plannedMillis - remainingMillis;
            if (elapsed < 0) elapsed = 0;
            baseActualMillis = elapsed;
        }

        remainingMillis = 0L;
        setupOvertimeTimerIfNeeded();
        // Startzustand der Auswahl-Views
        updateDurationsText();  // eine Methode aus SessionSummaryActivity kannst du hierher holen

    }

    private void initSummaryView() {
        if (summaryContainer == null) return;
        // Summary-Views
        durationsView = summaryContainer.findViewById(R.id.text_summary_durations);
        tvIgnoreOvertime = summaryContainer.findViewById(R.id.ignore_overtime);
        btnSave = summaryContainer.findViewById(R.id.button_save);
        // btnSaveActual = summaryContainer.findViewById(R.id.button_save_actual);
        Button btnSkipSave = summaryContainer.findViewById(R.id.button_skip_save);



        tvIgnoreOvertime.setOnClickListener(v -> {
            ignoreOvertime = true;
            tvIgnoreOvertime.setVisibility(GONE);

        });


        btnSave.setOnClickListener(v -> {
            long durationToStore = baseActualMillis;
            if (allowOvertime & !ignoreOvertime) {
                durationToStore = baseActualMillis + currentOvertimeMillis;
            }
            saveSession(durationToStore);
        });

        btnSkipSave.setOnClickListener(v -> finishAndGoNext());


    }


    private void setupOvertimeTimerIfNeeded() {
        if (!allowOvertime) {
            return;
        }

        long elapsedSinceFinish = System.currentTimeMillis() - finishTimestamp;
        if (elapsedSinceFinish < 0L) elapsedSinceFinish = 0L;
        currentOvertimeMillis = elapsedSinceFinish;

        overtimeTimer = new CountDownTimer(Long.MAX_VALUE, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentOvertimeMillis += 1000L;
                updateDurationsText();
            }

            @Override
            public void onFinish() {
                // praktisch nie
            }
        };
        overtimeTimer.start();
    }

    private void saveSession(long durationMillisToStore) {
        SessionLogEntry entry = new SessionLogEntry();
        entry.timestamp = System.currentTimeMillis();
        entry.practiceId = practice.getId();
       // entry.practiceName = practice.getName();
        entry.plannedMinutes = plannedMinutes;
        entry.actualMillis = durationMillisToStore;;
        entry.innerAfter = innerAfter;

        SessionLogManager.addSession(this, entry);
        finishAndGoNext();
    }

    private void finishAndGoNext() {
        if (overtimeTimer != null) {
            overtimeTimer.cancel();
            overtimeTimer = null;
        }

        endSessionCleanup();   // <-- ADD THIS

        switchMode(Mode.SETUP);
    }




    // endregion


// region Shared Methods

    private void switchMode(Mode newMode) {
        if (setupContainer != null) {
            setupContainer.setVisibility(newMode == Mode.SETUP ? VISIBLE : GONE);
        }
        if (runContainer != null) {
            runContainer.setVisibility(newMode == Mode.RUN ? VISIBLE : GONE);
        }
        if (summaryContainer != null) {
            summaryContainer.setVisibility(newMode == Mode.SUMMARY ? VISIBLE : GONE);
        }
    }


    private void resetTextStyle(TextView v) {
        if (v == null) return;
        v.setAllCaps(false);
        v.setTypeface(null, Typeface.NORMAL);
        v.setBackgroundResource(R.drawable.quickcheck_button_default);
        v.setTextColor(getResources().getColor(android.R.color.black));
    }

    private void setSelectedTextStyle(TextView v) {
        if (v == null) return;
        v.setTypeface(null, Typeface.BOLD);
        v.setBackgroundResource(R.drawable.quickcheck_button_selected);
        v.setTextColor(getResources().getColor(android.R.color.black));
    }

    //endregion

    @Override
    protected void onResume() {
        super.onResume();
        reloadPracticeSettings();
    }

    private void reloadPracticeSettings() {
        preMeditationCountdownSeconds = AppSettings.getPremeditationCountdownSec(this);

        vibrationOnStartEnd = AppSettings.isVibrationOnStartEndEnabled(this);
        flashOnStartEnd =  AppSettings.isFlashOnStartEndEnabled(this);
        preMeditationCountdownSeconds = AppSettings.getPremeditationCountdownSec(this);
        useDndDuringSession = AppSettings.isDndEnabled(this);
        featureCandleTimer = AppSettings.isCandleEnabled(this);
    }

}
