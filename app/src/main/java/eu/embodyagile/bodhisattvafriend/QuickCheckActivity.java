package eu.embodyagile.bodhisattvafriend;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.helper.LocaleHelper;
import eu.embodyagile.bodhisattvafriend.logic.InnerCondition;
import eu.embodyagile.bodhisattvafriend.logic.QuickCheckAnswers;
import eu.embodyagile.bodhisattvafriend.logic.TimeAvailable;
import eu.embodyagile.bodhisattvafriend.logic.VowManager;

public class QuickCheckActivity extends BaseActivity {

    private enum Step {
        PAUSE,
        STATE,
        TIME
    }
    private TextView goHomeView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pauseDoneRunnable;
    private Step step = Step.PAUSE;

    private View pauseContainer;
    private View stateContainer;
    private View timeContainer;

    private static final String PREFS_NAME = "bodhisattva_prefs";
    private static final String KEY_LAST_QC_PAUSE = "last_qc_pause_timestamp";
    private static final long QC_PAUSE_INTERVAL_MS = /* 60 * */  3 * 60 * 1000L; // 1 Stunde bzw für Test 5 minuten

    // Innere Zustände als TextViews
    private TextView tvInnerCuriousOpen;
    private TextView tvInnerExhausted;
    private TextView tvInnerSomewhatStressed;
    private TextView tvInnerStronglyTriggered;



    // Zeit-Slider
  //  private SeekBar seekTime;

    private TextView labelShort;
    private TextView labelMedium;
            private TextView labelLong;
    // Weiter-Button
    private Button btnNext;

    private InnerCondition selectedInnerCondition = null;
    private TimeAvailable selectedTime = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_check);
        pauseContainer = findViewById(R.id.layout_qc_pause);
        stateContainer = findViewById(R.id.layout_qc_state);
        timeContainer = findViewById(R.id.layout_qc_time);
        initPauseStep();
        initStateStep();
        initTimeStep();
        updateInnerViews();
        decideFirstStep();
    }

    private void initPauseStep() {
        if (pauseContainer == null) return;

        goHomeView = pauseContainer.findViewById(R.id.text_qc_go_home);

        goHomeView.setOnClickListener(v -> {
            // zurück zur MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
    private void startPauseCountdown() {

        pauseDoneRunnable = () -> switchStep(Step.STATE);

        handler.postDelayed(pauseDoneRunnable, 10_000L);
    }
    private void initTimeStep() {
        // Zeit-Slider
     //   seekTime = findViewById(R.id.seek_time);

        labelShort = findViewById(R.id.text_time_label_short);
        labelMedium = findViewById(R.id.text_time_label_medium);
        labelLong = findViewById(R.id.text_time_label_long);

        labelShort.setOnClickListener(v -> {

            selectedTime = TimeAvailable.SHORT;
            goToPractice();

        });

        labelMedium.setOnClickListener(v -> {
            selectedTime = TimeAvailable.MEDIUM;       goToPractice();    });

        labelLong.setOnClickListener(v -> {
            selectedTime = TimeAvailable.LONG;
        goToPractice();});
    }

    private void goToPractice() {
        QuickCheckAnswers answers = collectAnswers();

        PracticeRepository repo = PracticeRepository.getInstance(); // oder wie du ihn erzeugst

        String practiceId = PracticeRepository.recommendPracticeId(answers);

        Intent intent = new Intent(QuickCheckActivity.this, MeditationActivity.class);
        intent.putExtra(MeditationActivity.EXTRA_PRACTICE_ID, practiceId);
        intent.putExtra(MeditationActivity.EXTRA_PRE_TIME, answers.getTimeAvailable().name());
        intent.putExtra(MeditationActivity.EXTRA_PRE_INNER, answers.getInnerCondition().name());

        startActivity(intent);
    };
    private void initStateStep() {
        // Innere Zustände
        tvInnerCuriousOpen = findViewById(R.id.text_inner_curious_open);
        tvInnerExhausted = findViewById(R.id.text_inner_exhausted);
        tvInnerSomewhatStressed = findViewById(R.id.text_inner_somewhat_stressed);
        tvInnerStronglyTriggered = findViewById(R.id.text_inner_strongly_triggered);

        // Innere Zustände: TextViews klickbar machen
        tvInnerCuriousOpen.setOnClickListener(v -> {
            selectedInnerCondition = InnerCondition.CURIOUS_OPEN;
            updateInnerViews();
            switchStep(Step.TIME);
        });

        tvInnerExhausted.setOnClickListener(v -> {
            selectedInnerCondition = InnerCondition.EXHAUSTED_OVERWHELMED;
            updateInnerViews();
            switchStep(Step.TIME);

        });

        tvInnerSomewhatStressed.setOnClickListener(v -> {
            selectedInnerCondition = InnerCondition.SOMEWHAT_STRESSED;
            updateInnerViews();
            switchStep(Step.TIME);

        });

        tvInnerStronglyTriggered.setOnClickListener(v -> {
            selectedInnerCondition = InnerCondition.STRONGLY_TRIGGERED;
            updateInnerViews();
            switchStep(Step.TIME);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && pauseDoneRunnable != null) {
            handler.removeCallbacks(pauseDoneRunnable);
        }
    }
    private void decideFirstStep() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lastShown = prefs.getLong(KEY_LAST_QC_PAUSE, 0L);
        long now = System.currentTimeMillis();

        if (now - lastShown > QC_PAUSE_INTERVAL_MS) {
            // Pause zeigen
            switchStep(Step.PAUSE);

            // Zeitstempel updaten
            prefs.edit().putLong(KEY_LAST_QC_PAUSE, now).apply();

            startPauseCountdown();
        } else {
            // direkt zu innerem Zustand
            switchStep(Step.STATE);
        }
    }

    private void switchStep(Step newStep) {
        step = newStep;

        if (pauseContainer != null) {
            pauseContainer.setVisibility(newStep == Step.PAUSE ? View.VISIBLE : View.GONE);
        }
        if (stateContainer != null) {
            stateContainer.setVisibility(newStep == Step.STATE ? View.VISIBLE : View.GONE);
        }
        if (timeContainer != null) {
            timeContainer.setVisibility(newStep == Step.TIME ? View.VISIBLE : View.GONE);
        }
    }




    private QuickCheckAnswers collectAnswers() {
        if (selectedInnerCondition == null || selectedTime == null) {
            return null;
        }
        return new QuickCheckAnswers(selectedInnerCondition, selectedTime);
    }

    private void updateInnerViews() {
        resetTextStyle(tvInnerCuriousOpen);
        resetTextStyle(tvInnerExhausted);
        resetTextStyle(tvInnerSomewhatStressed);
        resetTextStyle(tvInnerStronglyTriggered);

        if (selectedInnerCondition == InnerCondition.CURIOUS_OPEN) {
            setSelectedTextStyle(tvInnerCuriousOpen);
        } else if (selectedInnerCondition == InnerCondition.EXHAUSTED_OVERWHELMED) {
            setSelectedTextStyle(tvInnerExhausted);
        } else if (selectedInnerCondition == InnerCondition.SOMEWHAT_STRESSED) {
            setSelectedTextStyle(tvInnerSomewhatStressed);
        } else if (selectedInnerCondition == InnerCondition.STRONGLY_TRIGGERED) {
            setSelectedTextStyle(tvInnerStronglyTriggered);
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



    private void resetQuickCheck() {
        // clear selections
        selectedInnerCondition = null;
        selectedTime = null;


        // run your logic again to decide if Pause should be shown
       // decideFirstStep();  // the method that checks "pause shown in last hour?"
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetQuickCheck();
    }


}
