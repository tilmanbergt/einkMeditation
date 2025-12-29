package eu.embodyagile.bodhisattvafriend.helper;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.Window;
import android.view.WindowManager;

public class FeedbackHelper {

    private final Context appContext;

    public FeedbackHelper(Context context) {
        this.appContext = context.getApplicationContext();
    }

    // --- VIBRATION -------------------------------------------------------

    public void vibrateShort() {
        Vibrator vibrator = getVibrator();
        if (vibrator == null || !vibrator.hasVibrator()) {
            return; // device has no vibrator – fail silently
        }

        // Min SDK is 31, so VibrationEffect is always available
        VibrationEffect effect = VibrationEffect.createOneShot(
                150, // duration in ms
                VibrationEffect.DEFAULT_AMPLITUDE
        );
        vibrator.vibrate(effect);
    }

    private Vibrator getVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm =
                    (VibratorManager) appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                return vm.getDefaultVibrator();
            }
        }
        return (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    // --- BACKLIGHT FLASH -------------------------------------------------

    /**
     * Briefly increases the activity window brightness and then restores it.
     * On an E-Ink device this should correspond to the front-/backlight.
     */
    public void flashBacklight(Activity activity, long durationMs) {
        if (activity == null) return;

        Window window = activity.getWindow();
        if (window == null) return;

        WindowManager.LayoutParams lp = window.getAttributes();
        final float originalBrightness = lp.screenBrightness; // -1 = system default

        // bump brightness to max (1.0f)
        lp.screenBrightness = 1.0f;
        window.setAttributes(lp);

        // restore after durationMs
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Window w = activity.getWindow();
            if (w == null) return;

            WindowManager.LayoutParams lpRestore = w.getAttributes();
            lpRestore.screenBrightness = originalBrightness;
            w.setAttributes(lpRestore);
        }, durationMs);
    }
}
