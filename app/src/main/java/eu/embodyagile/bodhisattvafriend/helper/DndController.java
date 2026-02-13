package eu.embodyagile.bodhisattvafriend.helper;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class DndController {

    private final Activity activity;
    private final NotificationManager nm;

    private boolean enabled;
    private int prevFilter = -1;

    public DndController(Activity activity, NotificationManager nm) {
        this.activity = activity;
        this.nm = nm;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasDndAccess() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || (nm != null && nm.isNotificationPolicyAccessGranted());
    }

    public void openDndAccessSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        activity.startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
    }

    public void applyIfEnabled() {
        if (!enabled) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        if (nm == null) return;
        if (!nm.isNotificationPolicyAccessGranted()) return;

        if (prevFilter == -1) prevFilter = nm.getCurrentInterruptionFilter();

        // keep same behavior as before
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
    }

    public void restoreIfChanged() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        if (nm == null) return;

        if (prevFilter != -1 && nm.isNotificationPolicyAccessGranted()) {
            nm.setInterruptionFilter(prevFilter);
        }
        prevFilter = -1;
    }
}
