package eu.embodyagile.bodhisattvafriend.helper;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class CandleUiController {

    public interface Dimmer {
        void setDim(boolean dark);
        boolean isDimEnabled();
    }

    private static final long FIRST_10S_MS = 10_000L;
    private static final long TEMP_SHOW_MS = 5_000L;

    private final Activity activity;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private boolean featureEnabled = true;

    private ImageView candleView;
    private TextView timerView;

    private boolean persistentTimeMode = false;

    private Dimmer dimmer;

    public CandleUiController(Activity activity) {
        this.activity = activity;
    }

    public void setFeatureEnabled(boolean enabled) {
        this.featureEnabled = enabled;
    }

    public void setDimmer(Dimmer dimmer) {
        this.dimmer = dimmer;
    }

    public void bind(ImageView candleView, TextView timerView) {
        this.candleView = candleView;
        this.timerView = timerView;

        if (candleView == null || timerView == null) return;

        if (!featureEnabled) {
            candleView.setVisibility(View.GONE);
            timerView.setVisibility(View.VISIBLE);
            return;
        }

        candleView.setClickable(true);
        timerView.setClickable(true);

        final GestureDetector candleDetector = new GestureDetector(activity,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent e) { return true; }

                    @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                        showTimeTemporarily();
                        return true;
                    }

                    @Override public boolean onDoubleTap(MotionEvent e) {
                        persistentTimeMode = true;
                        setRunDisplayTime();
                        return true;
                    }
                });

        candleView.setOnTouchListener((v, ev) -> {
            candleDetector.onTouchEvent(ev);
            if (ev.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return true;
        });

        final GestureDetector timeDetector = new GestureDetector(activity,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent e) { return true; }

                    @Override public boolean onDoubleTap(MotionEvent e) {
                        persistentTimeMode = false;
                        uiHandler.removeCallbacks(revertTempTimeToCandle);
                        setRunDisplayCandle();
                        return true;
                    }
                });

        timerView.setOnTouchListener((v, ev) -> {
            timeDetector.onTouchEvent(ev);
            if (ev.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return true;
        });
    }

    /** call when pretimer starts */
    public void onPreTimerStart() {
        if (!featureEnabled) return;
        persistentTimeMode = false;
        uiHandler.removeCallbacks(showCandleAfterFirst10s);
        uiHandler.removeCallbacks(revertTempTimeToCandle);
        setRunDisplayTime();
    }

    /** call when meditation starts */
    public void onMeditationStart() {
        if (!featureEnabled) {
            setRunDisplayTime();
            return;
        }
        persistentTimeMode = false;
        setRunDisplayTime();
        uiHandler.removeCallbacks(showCandleAfterFirst10s);
        uiHandler.postDelayed(showCandleAfterFirst10s, FIRST_10S_MS);
    }

    public void cleanup() {
        uiHandler.removeCallbacks(showCandleAfterFirst10s);
        uiHandler.removeCallbacks(revertTempTimeToCandle);
    }

    private final Runnable showCandleAfterFirst10s = () -> {
        if (!featureEnabled) return;
        if (!persistentTimeMode) setRunDisplayCandle();
    };

    private final Runnable revertTempTimeToCandle = () -> {
        if (!featureEnabled) return;
        if (!persistentTimeMode) {
            setRunDisplayCandle();
            if (dimmer != null && dimmer.isDimEnabled()) dimmer.setDim(true);
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
        if (!featureEnabled) return;

        if (persistentTimeMode) {
            setRunDisplayTime();
            return;
        }

        setRunDisplayTime();
        if (dimmer != null && dimmer.isDimEnabled()) dimmer.setDim(false);

        uiHandler.removeCallbacks(revertTempTimeToCandle);
        uiHandler.postDelayed(revertTempTimeToCandle, TEMP_SHOW_MS);
    }
}
