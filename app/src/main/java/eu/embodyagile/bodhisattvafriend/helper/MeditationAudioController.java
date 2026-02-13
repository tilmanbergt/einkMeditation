package eu.embodyagile.bodhisattvafriend.helper;

import android.content.Context;
import android.media.MediaPlayer;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.model.Practice;

public class MeditationAudioController {

    private final Context ctx;

    private MediaPlayer audioPlayer;
    private boolean isPrepared = false;

    public MeditationAudioController(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public void start(Practice practice) {
        if (practice == null) return;

        Practice.AudioConfig audio = practice.getAudioConfig();
        if (audio == null) return;

        int resId = PracticeRepository.getInstance().resolveAudioResId(audio);
        if (resId == 0) return;

        stop(); // safety

        audioPlayer = MediaPlayer.create(ctx, resId);
        if (audioPlayer == null) return;

        audioPlayer.setLooping(audio.isLoop());
        audioPlayer.setOnPreparedListener(mp -> {
            isPrepared = true;
            mp.start();
        });

        // create() is typically already prepared
        isPrepared = true;
        audioPlayer.start();
    }

    public void pauseIfPlaying() {
        if (audioPlayer == null) return;
        if (audioPlayer.isPlaying()) audioPlayer.pause();
    }

    public void resumeIfPossible() {
        if (audioPlayer == null) return;
        try {
            if (!audioPlayer.isPlaying()) audioPlayer.start();
        } catch (Exception ignored) {
        }
    }

    public boolean isUsableForPauseResume() {
        return audioPlayer != null && isPrepared;
    }

    public void stop() {
        if (audioPlayer == null) return;
        try {
            if (audioPlayer.isPlaying()) audioPlayer.stop();
        } catch (Exception ignored) {
        }
        try {
            audioPlayer.reset();
        } catch (Exception ignored) {
        }
        try {
            audioPlayer.release();
        } catch (Exception ignored) {
        }
        audioPlayer = null;
        isPrepared = false;
    }
}
