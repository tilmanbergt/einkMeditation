package eu.embodyagile.bodhisattvafriend.logic;

import android.content.Context;
import android.content.SharedPreferences;

public class VowManager {

    private static final String PREFS_NAME = "bodhisattva_friend_prefs";
    private static final String KEY_CORE_VOW = "core_vow_text";

    public static String getCoreVow(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String vow = prefs.getString(KEY_CORE_VOW, "");
        return vow != null ? vow.trim() : "";
    }

    public static void setCoreVow(Context context, String vow) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CORE_VOW, vow != null ? vow : "")
                .apply();
    }
}
