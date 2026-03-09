package eu.embodyagile.bodhisattvafriend.helper;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import eu.embodyagile.bodhisattvafriend.R;
import eu.embodyagile.bodhisattvafriend.settings.AppSettings;

public class IntervalBellController {

    private final Context appContext;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean running = false;
    private long intervalMs = 0L;
    private long plannedDurationMs = -1L;

    private Runnable scheduledRunnable;

    public IntervalBellController(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void startIfEnabled(long plannedDurationMs) {
        stop();

        if (!AppSettings.isIntervalBellEnabled(appContext)) return;

        int minutes = AppSettings.getIntervalBellMinutes(appContext);
        intervalMs = minutes * 60L * 1000L;
        if (intervalMs <= 0) return;

        this.plannedDurationMs = plannedDurationMs;
        this.running = true;

        scheduleNextBellFromElapsed(0L);
    }

    public void pause() {
        running = false;
        if (scheduledRunnable != null) {
            handler.removeCallbacks(scheduledRunnable);
            scheduledRunnable = null;
        }
    }

    public void resumeFromElapsedIfEnabled(long elapsedMeditationMs) {
        if (!AppSettings.isIntervalBellEnabled(appContext)) return;
        if (intervalMs <= 0) return;

        running = true;
        scheduleNextBellFromElapsed(elapsedMeditationMs);
    }

    public void stop() {
        running = false;
        if (scheduledRunnable != null) {
            handler.removeCallbacks(scheduledRunnable);
            scheduledRunnable = null;
        }
        plannedDurationMs = -1L;
        intervalMs = 0L;
    }

    private void scheduleNextBellFromElapsed(long elapsedMeditationMs) {
        if (!running || intervalMs <= 0) return;

        long remainder = elapsedMeditationMs % intervalMs;
        long delayMs = (remainder == 0) ? intervalMs : (intervalMs - remainder);

        scheduledRunnable = () -> {
            if (!running) return;

            long nextBellElapsedMs = elapsedMeditationMs + delayMs;

            boolean collidesWithPlannedEnd =
                    plannedDurationMs > 0 && nextBellElapsedMs == plannedDurationMs;

            if (!collidesWithPlannedEnd) {
                playIntervalBell();
            }

            scheduleNextBellFromElapsed(nextBellElapsedMs);
        };

        handler.postDelayed(scheduledRunnable, delayMs);
    }

    private void playIntervalBell() {
        try {
            MediaPlayer mp = MediaPlayer.create(appContext, R.raw.triangle);
            if (mp == null) return;

            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setOnCompletionListener(player -> {
                try {
                    player.release();
                } catch (Exception ignored) {}
            });
            mp.start();
        } catch (Exception ignored) {}
    }
}