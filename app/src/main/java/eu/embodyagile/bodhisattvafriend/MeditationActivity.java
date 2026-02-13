package eu.embodyagile.bodhisattvafriend;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.NotificationManager;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.data.SessionState;
import eu.embodyagile.bodhisattvafriend.helper.CandleUiController;
import eu.embodyagile.bodhisattvafriend.helper.DndController;
import eu.embodyagile.bodhisattvafriend.helper.LightAndSoundHelper;
import eu.embodyagile.bodhisattvafriend.helper.MeditationAudioController;
import eu.embodyagile.bodhisattvafriend.history.SessionLogEntry;
import eu.embodyagile.bodhisattvafriend.history.SessionLogManager;
import eu.embodyagile.bodhisattvafriend.logic.InnerCondition;
import eu.embodyagile.bodhisattvafriend.model.Practice;
import eu.embodyagile.bodhisattvafriend.settings.AppSettings;


public class MeditationActivity extends BaseActivity {

    // for log generation
    private static final String TAG = "MEDI_FLOW";

    public static final String EXTRA_PRE_TIME = "extra_pre_time"; // nur für MeditationSelectionActivity
    public static final String EXTRA_PRE_INNER = "extra_pre_inner";// nur für MeditationSelectionActivity
    private boolean useDndDuringSession = true;
    private DndController dnd;
    private MeditationAudioController audio;
    private CandleUiController candleUi;


    //region general fields
    private boolean playAudio = false;
    private LightAndSoundHelper lightAndSoundHelper;


    //Pomodorro timer link in
    public static final String EXTRA_FROM_POMODORO = "extra_from_pomodoro";
    private boolean fromPomodoro = false;

    public static final String EXTRA_SELECTED_MINUTES = "extra_selected_minutes";
    public static final String EXTRA_PLAY_AUDIO = "extra_play_audio"; // optional, aber praktisch

    //for dimming during meditation.


    private boolean dimDuringMeditation;

    public static final String EXTRA_PRACTICE_ID = "extra_practice_id";

    private Practice practice;

    private boolean ignoreOvertime = false;

    private enum Mode {
        RUN, SUMMARY
    }

    private final SessionState session = new SessionState();
    private View runContainer;
    private View summaryContainer;

    //endregion

    // Fields
    private TextView instructionView;
    private TextView timerView;
    private Button pauseContinueButton;
    private Button endButton;
    private Button cancelButton;

    //Logic

    private CountDownTimer timer;
    private CountDownTimer preMeditationTimer;


    private MediaPlayer bellPlayer;

//endregion

    //region Summary Views Fields
    private Button btnSave;
    // Feature flag (disable candle functionality by setting false)
    private boolean featureCandleTimer = true;

    // Views
    private android.widget.ImageView candleView;

    // private TextView practiceNameView;
    private TextView durationsView;
    private TextView tvIgnoreOvertime;
    // Logic
    private InnerCondition innerAfter = null;
    //  private boolean goToVowAfter = false;


    private CountDownTimer overtimeTimer;
    private boolean vibrationOnStartEnd;
    private boolean flashOnStartEnd;
    private int preMeditationCountdownSeconds;


// endregion

    // region Create and Destroy Methods


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meditation);

        // Intents einlesen
        fromPomodoro = getIntent().getBooleanExtra(EXTRA_FROM_POMODORO, false);
        int minutes = getIntent().getIntExtra(EXTRA_SELECTED_MINUTES, -1);
        playAudio = getIntent().getBooleanExtra(EXTRA_PLAY_AUDIO, playAudio);
        String practiceId = getIntent().getStringExtra(EXTRA_PRACTICE_ID);

        if (minutes <= 0) { //no meditation needed!
            finish();
            return;
        } else session.setPlannedMinutes(minutes);


        // Helperklassen
        dnd = new DndController(this, (NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        audio = new MeditationAudioController(this);
        candleUi = new CandleUiController(this);
        candleUi.setDimmer(new CandleUiController.Dimmer() {
            @Override
            public void setDim(boolean dark) {
                lightAndSoundHelper.setMeditationDarkMode(dark, MeditationActivity.this, MeditationActivity.this);
            }

            @Override
            public boolean isDimEnabled() {
                return dimDuringMeditation;
            }
        });

        lightAndSoundHelper = new LightAndSoundHelper(this);

        // Practice Bestimmen aus repository
        reloadPracticeSettings();
        PracticeRepository repository = PracticeRepository.getInstance();
        repository.init(this);   // <-- WICHTIG, jedes Mal aufrufen für sprachewechsel
        practice = repository.getPracticeById(practiceId);
        if (practice == null) {
            practice = repository.getFallbackPractice();
            if (practice == null) {finish(); return;}
        }

        // Lautstärke-Tasten steuern Klangschale
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //Allgemeinen View setzen
        TextView nameView = findViewById(R.id.text_practice_name);
        nameView.setText(practice.getName());


        // Layout-Container
        runContainer = findViewById(R.id.layout_session);
        summaryContainer = findViewById(R.id.layout_summary);

        // initial nur Run zeigen
        switchMode(Mode.RUN);

        // Views initialisieren
        initRunView();
        initSummaryView();

        //pretimer oder timer starten
        if (session.plannedMinutes() > 0) {
            if (preMeditationCountdownSeconds > 0)
                enterPreTimer();
            else
                enterMeditationRunning(true);
            return;
        }

        //fallback
        finish();
    }


    private void saveAndFinishPomodoroSession() {
        if (!session.isFinished()) {
            // defensive fallback; normally shouldn't happen
            Log.w(TAG, "saveAndFinishPomodoroSession called but session not finished; forcing EARLY_END");
            session.finishEarly();
        }
        signalStartEnd();

        long durationToStore = session.durationToStoreMillis(); // or durationToStoreMillis(false) if you want no overtime in pomodoro
        SessionLogEntry entry = new SessionLogEntry();
        entry.timestamp = System.currentTimeMillis();
        entry.practiceId = practice.getId();
        entry.plannedMinutes = session.plannedMinutes();
        entry.actualMillis = durationToStore;
        entry.innerAfter = innerAfter;

        SessionLogManager.addSession(this, entry);
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupAllResources();
    }

    //endregion


    // region Setup View Methods


    private void onCancelPretimerClicked() {
        if (preMeditationTimer != null) {
            preMeditationTimer.cancel();
            preMeditationTimer = null;
        }
        cleanupSessionEffects();


        if (fromPomodoro) {
            finish(); // zurück zu Pomodoro
            return;
        }

        // sonst zurück ins Setup
        Intent i = new Intent(this, MeditationSetupActivity.class);
        i.putExtra(MeditationSetupActivity.EXTRA_PRACTICE_ID, practice.getId());
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
        finish();
    }

    private void updateDurationsText() {
        if (durationsView == null) return;

        long baseSeconds = session.baseDurationSeconds();
        String text = formatMinutesSeconds(baseSeconds);

        if (session.canOvertime()) {
            long overtimeSeconds = ignoreOvertime ? 0 : session.overtimeSecondsNowIfAllowed();
            if (overtimeSeconds > 5) {
                text += " +" + formatMinutesSeconds(overtimeSeconds);
                tvIgnoreOvertime.setVisibility(VISIBLE);
            } else {
                tvIgnoreOvertime.setVisibility(GONE);
            }
        } else {
            tvIgnoreOvertime.setVisibility(GONE);
        }

        durationsView.setText(text);
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
        candleUi.bind(candleView, timerView);

        // ✅ ADD: listeners must be set here (not in setupPracticeUI)
        if (pauseContinueButton != null)
            pauseContinueButton.setOnClickListener(v -> onPauseContinueClicked());
        if (endButton != null) endButton.setOnClickListener(v -> onEndClicked());
        if (cancelButton != null) cancelButton.setOnClickListener(v -> onCancelPretimerClicked());
        if (instructionView != null && practice != null) {
            instructionView.setText(practice.getInstructionText());
        }
        if (pauseContinueButton != null) {
            pauseContinueButton.setVisibility(fromPomodoro ? GONE : VISIBLE);
        }

        // Ensure touch works even if something else changes later
        candleView.setClickable(true);
        timerView.setClickable(true);

    }

    private void onPauseContinueClicked() {
        if (fromPomodoro) return;

        if (session.isRunning()) {
            session.pause();
            pauseTimerUI();
            if (playAudio && audio.isUsableForPauseResume()) audio.pauseIfPlaying();
            return;
        }

        if (session.isPaused()) {
            session.resume();              // <- statt start()
            startTimerUI(false);
            if (playAudio && audio.isUsableForPauseResume()) audio.resumeIfPossible();
        }
    }


    private void onEndClicked() {
        // nur reagieren, wenn jemals gestartet wurde
        if (!session.isRunning() && !session.isPaused()) {
            return;
        }

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        session.finishEarly();

        if (fromPomodoro) {
            saveAndFinishPomodoroSession(); // early end
        } else {
            onSessionFinished();
        }
    }

    private void startPreMeditationCountdown() {

        preMeditationTimer = new CountDownTimer(preMeditationCountdownSeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000L;
                timerView.setText("-" + String.format("%02d", seconds));
            }

            @Override
            public void onFinish() {

                enterMeditationRunning(true);

            }
        };
        preMeditationTimer.start();
    }

    @Override
    protected void onStop() {
        if (!isChangingConfigurations()) cleanupSessionEffects();

        super.onStop();
    }


    private void startTimerUI(boolean signalStart) {
        if (timer != null) timer.cancel();

        if (signalStart) signalStartEnd();
        if (dimDuringMeditation) lightAndSoundHelper.setMeditationDarkMode(true, this, this);

        dnd.setEnabled(useDndDuringSession);
        if (useDndDuringSession) {
            if (!dnd.hasDndAccess()) dnd.openDndAccessSettings();
            else dnd.applyIfEnabled();
        }

        // UI tick: jede Sekunde Anzeige aktualisieren
        timer = new CountDownTimer(Long.MAX_VALUE, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerTextFromSession();
                if (session.remainingMillisNow() <= 0 && !session.isFinished()) {
                    session.finishNatural();
                    onSessionFinished();
                }
            }

            @Override
            public void onFinish() {
            }
        };
        timer.start();
    }

    private void updateTimerTextFromSession() {
        long remainingSeconds = session.remainingSecondsNow();
        timerView.setText(formatMinutesSeconds(remainingSeconds));
    }

    private void pauseTimerUI() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        pauseContinueButton.setText(getString(R.string.weiter));
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
            lightAndSoundHelper.vibrateShort();
        }
        if (flashOnStartEnd) {
            lightAndSoundHelper.flashBacklight(this, 500);
        }
    }

    // endregion

    // region Summary View Methods


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
            saveSession();
        });

        btnSkipSave.setOnClickListener(v -> finishAndGoNext());
    }


    private void setupOvertimeTimerIfNeeded() {
        if (!session.canOvertime()) return;

        if (overtimeTimer != null) {
            overtimeTimer.cancel();
            overtimeTimer = null;
        }

        overtimeTimer = new CountDownTimer(Long.MAX_VALUE, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateDurationsText();
            }

            @Override
            public void onFinish() {
            }
        };
        overtimeTimer.start();
    }


    private void saveSession() {
        long durationMillisToStore = session.durationToStoreMillis(!ignoreOvertime);
        SessionLogEntry entry = new SessionLogEntry();
        entry.timestamp = System.currentTimeMillis();
        entry.practiceId = practice.getId();
        // entry.practiceName = practice.getName();
        entry.plannedMinutes = session.plannedMinutes();
        entry.actualMillis = durationMillisToStore;

        entry.innerAfter = innerAfter;
        Log.d(TAG, "saveSession: plannedMinutes=" + session.plannedMinutes() + " allowOvertime=" + session.canOvertime() + " ignoreOvertime=" + ignoreOvertime + " storedMillis=" + durationMillisToStore);
        SessionLogManager.addSession(this, entry);

        finishAndGoNext();
    }

    private void finishAndGoNext() {
        if (overtimeTimer != null) {
            overtimeTimer.cancel();
            overtimeTimer = null;
        }
        cleanupSessionEffects();

        Intent intent = new Intent(MeditationActivity.this, HistoryActivity.class);
        startActivity(intent);
        switchMode(Mode.RUN); //go to history, but prepare maditation screen again
    }


    // endregion

// region Shared Methods


    private void switchMode(Mode newMode) {

        if (runContainer != null) {
            runContainer.setVisibility(newMode == Mode.RUN ? VISIBLE : GONE);
        }
        if (summaryContainer != null) {
            summaryContainer.setVisibility(newMode == Mode.SUMMARY ? VISIBLE : GONE);
        }
    }


    //endregion

    @Override
    protected void onResume() {
        super.onResume();
        reloadPracticeSettings();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }


    private void reloadPracticeSettings() {
        preMeditationCountdownSeconds = AppSettings.getPremeditationCountdownSec(this);
        dimDuringMeditation = AppSettings.isDimDuringMeditationEnabled(this);

        vibrationOnStartEnd = AppSettings.isVibrationOnStartEndEnabled(this);
        flashOnStartEnd = AppSettings.isFlashOnStartEndEnabled(this);
        preMeditationCountdownSeconds = AppSettings.getPremeditationCountdownSec(this);
        useDndDuringSession = AppSettings.isDndEnabled(this);
        featureCandleTimer = AppSettings.isCandleEnabled(this);
        if (dnd != null) dnd.setEnabled(useDndDuringSession);
        if (candleUi != null) candleUi.setFeatureEnabled(featureCandleTimer);
    }


    private void enterPreTimer() {
        endButton.setVisibility(GONE);
        cancelButton.setVisibility(VISIBLE);
        pauseContinueButton.setVisibility(GONE);
        candleUi.setFeatureEnabled(featureCandleTimer);
        candleUi.onPreTimerStart();
        startPreMeditationCountdown(); // nur Timer starten, kein UI mehr
    }

    private void enterMeditationRunning(boolean signalStart) {
        endButton.setVisibility(VISIBLE);
        cancelButton.setVisibility(GONE);
        pauseContinueButton.setVisibility(fromPomodoro ? GONE : VISIBLE);
        pauseContinueButton.setText(getString(R.string.pause));
        if (playAudio) audio.start(practice);

        candleUi.setFeatureEnabled(featureCandleTimer);
        candleUi.onMeditationStart();

        session.start();           // nur beim echten Start
        startTimerUI(signalStart);
    }

    private void onSessionFinished() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        signalStartEnd();
        cleanupSessionEffects();
        if (fromPomodoro) {
            saveAndFinishPomodoroSession();
            return;
        }
        switchMode(Mode.SUMMARY);

        setupOvertimeTimerIfNeeded();
        updateDurationsText();
    }


    private void cleanupSessionEffects() {
        if (dnd != null) dnd.restoreIfChanged();
        if (candleUi != null) candleUi.cleanup();
        if (dimDuringMeditation) lightAndSoundHelper.setMeditationDarkMode(false, this, this);
        if (audio != null) audio.stop();
    }

    private void cleanupAllResources() {
        cleanupSessionEffects();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (preMeditationTimer != null) {
            preMeditationTimer.cancel();
            preMeditationTimer = null;
        }
        if (overtimeTimer != null) {
            overtimeTimer.cancel();
            overtimeTimer = null;
        }
        if (bellPlayer != null) {
            bellPlayer.release();
            bellPlayer = null;
        }
    }

    private static String formatMinutesSeconds(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long m = totalSeconds / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }

}
