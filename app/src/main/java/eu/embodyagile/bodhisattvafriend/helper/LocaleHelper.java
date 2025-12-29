package eu.embodyagile.bodhisattvafriend.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS_NAME = "bf_settings";
    private static final String KEY_LANGUAGE = "language";

    public static Context onAttach(Context context) {
        String lang = getPersistedLanguage(context, null);
        if (lang == null || lang.isEmpty()) {
            // keine Auswahl gespeichert → System-Sprache verwenden
            return context;
        }
        return setLocale(context, lang);
    }

    public static Context setLocale(Context context, String lang) {
        persistLanguage(context, lang);

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    private static void persistLanguage(Context context, String lang) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, lang).apply();
    }

    private static String getPersistedLanguage(Context context, String defaultLang) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, defaultLang);
    }

    public static String getCurrentLanguage(Context context) {
        return getPersistedLanguage(context, "");
    }

    public static void clearLanguage(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_LANGUAGE).apply();
    }
}
