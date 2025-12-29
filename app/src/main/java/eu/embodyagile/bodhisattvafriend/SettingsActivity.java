package eu.embodyagile.bodhisattvafriend;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.app.NotificationManager;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.helper.LocaleHelper;
import eu.embodyagile.bodhisattvafriend.settings.AppSettings;

public class SettingsActivity extends BaseActivity {
     private SwitchCompat switchCandle;
    private SwitchCompat switchDnd;
    private View settingsMainContainer;
    private View goalsContainer;

    private TextView buttonGoals;
    private Button buttonGoalsBack;


    private boolean pendingDndEnable = false;

    private View mainMenu, languageContainer, practiceContainer, managePracticesContainer, systemContainer;
    private TextView headerText;

    private RadioGroup langGroup;
    private RadioButton langSystem, langDe, langEn;
    private SwitchCompat switchVibration;
    private SwitchCompat switchFlash;
    private EditText preMeditationCountdownEdit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        settingsMainContainer = findViewById(R.id.settings_main_menu);
        goalsContainer = findViewById(R.id.goalsContainer);

        buttonGoalsBack = findViewById(R.id.button_goals_back);
              buttonGoalsBack.setOnClickListener(v -> showMainSettingsPage());

        headerText = findViewById(R.id.settings_header_text);
        mainMenu = findViewById(R.id.settings_main_menu);
        languageContainer = findViewById(R.id.settings_language_container);
        practiceContainer = findViewById(R.id.settings_practice_container);
        managePracticesContainer = findViewById(R.id.settings_manage_practices_container);
        systemContainer = findViewById(R.id.settings_system_container);

        setupMainMenu();
        setupLanguageSettings();
        setupPracticeSettings();
        setupGoalsSettings();
        boolean enabled = getResources().getBoolean(R.bool.config_feature_practicemanagement);
if (enabled) {
        setupManagePracticesSettings(); }
        setupSystemSettings();

        showSubPage(null); // Show main menu initially
    }

    private void setupMainMenu() {
        findViewById(R.id.button_language_settings).setOnClickListener(v -> showSubPage(languageContainer));
        findViewById(R.id.button_practice_settings).setOnClickListener(v -> showSubPage(practiceContainer));
        findViewById(R.id.button_system_settings).setOnClickListener(v -> showSubPage(systemContainer));
        buttonGoals = findViewById(R.id.button_goals_settings);

        buttonGoals.setOnClickListener(v -> showGoalsPage());

        TextView practiceManagement=  findViewById(R.id.button_manage_practices_settings);
        boolean enabled = getResources().getBoolean(R.bool.config_feature_practicemanagement);

        if (practiceManagement != null) {
            practiceManagement.setVisibility(enabled ? View.VISIBLE : View.GONE);
            if (enabled) {
                practiceManagement.setOnClickListener(v -> showSubPage(managePracticesContainer));
            }
        }

    }

    private void setupLanguageSettings() {
        langGroup = findViewById(R.id.langRadioGroup);
        langSystem = findViewById(R.id.langSystem);
        langDe = findViewById(R.id.langDe);
        langEn = findViewById(R.id.langEn);

        String current = LocaleHelper.getCurrentLanguage(this);
        if (current == null || current.isEmpty()) {
            langSystem.setChecked(true);
        } else if ("de".equals(current)) {
            langDe.setChecked(true);
        } else if ("en".equals(current)) {
            langEn.setChecked(true);
        }

        langGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.langSystem) {
                LocaleHelper.clearLanguage(this);
                recreateApp();
            } else if (checkedId == R.id.langDe) {
                LocaleHelper.setLocale(this, "de");
                recreateApp();
            } else if (checkedId == R.id.langEn) {
                LocaleHelper.setLocale(this, "en");
                recreateApp();
            }
        });

        findViewById(R.id.button_back_language).setOnClickListener(v -> showSubPage(null));
    }
    private void showGoalsPage() {
        settingsMainContainer.setVisibility(View.GONE);
        goalsContainer.setVisibility(View.VISIBLE);
    }

    private void showMainSettingsPage() {
        goalsContainer.setVisibility(View.GONE);
        settingsMainContainer.setVisibility(View.VISIBLE);
    }

    private void setupPracticeSettings() {
        switchVibration = findViewById(R.id.switch_vibration);
        switchFlash = findViewById(R.id.switch_flash);
        preMeditationCountdownEdit = findViewById(R.id.edit_pre_meditation_countdown);

        // NEW:
        switchCandle = findViewById(R.id.switch_candle);
        switchDnd = findViewById(R.id.switch_dnd);

        boolean vibrationOn = AppSettings.isVibrationOnStartEndEnabled(this);
        boolean flashOn = AppSettings.isFlashOnStartEndEnabled(this);

        switchVibration.setChecked(vibrationOn);
        switchFlash.setChecked(flashOn);

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> AppSettings.setVibrationOnStartEndEnabled(this,isChecked));

        switchFlash.setOnCheckedChangeListener((buttonView, isChecked) ->
                AppSettings.setFlashOnStartEndEnabled(this,isChecked));


        int countdown = AppSettings.getPremeditationCountdownSec(this);
        preMeditationCountdownEdit.setText(String.valueOf(countdown));

        preMeditationCountdownEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int value = 0;
                try {
                    String t = s.toString().trim();
                    if (!t.isEmpty()) value = Integer.parseInt(t);
                } catch (NumberFormatException ignored) {}
                AppSettings.setPremeditationCountdownSec(getApplicationContext(), value);

            }
        });

// optional safety-net (some keyboards don’t trigger change events the way you expect)
        preMeditationCountdownEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String t = preMeditationCountdownEdit.getText().toString().trim();
                int value = 0;
                try {
                    if (!t.isEmpty()) value = Integer.parseInt(t);
                } catch (NumberFormatException ignored) {}
                AppSettings.setPremeditationCountdownSec(this,value);
            }
        });


        // … keep your TextWatcher as-is …

        // ---- Candle default ON ----
        boolean candleOn = AppSettings.isCandleEnabled(this);
        switchCandle.setChecked(candleOn);
        switchCandle.setOnCheckedChangeListener((buttonView, isChecked) ->
                AppSettings.setCandleEnabled(this,isChecked));


        // ---- DND default OFF; only persist ON if access granted ----
        boolean dndOn = AppSettings.isDndEnabled(this);
        switchDnd.setChecked(dndOn);

        switchDnd.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                pendingDndEnable = false;
                AppSettings.setDndEnabled(this,false);
                return;
            }

            // user turned ON:
            if (hasDndAccess()) {
                AppSettings.setDndEnabled(this,true);
            } else {
                // do NOT store ON yet; send user to settings
                pendingDndEnable = true;
                switchDnd.setChecked(false); // keep UI honest until granted
                openDndAccessSettings();
                Toast.makeText(this, R.string.dnd_permission_needed, Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.button_back_practice).setOnClickListener(v -> showSubPage(null));
    }

    private boolean hasDndAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    private void openDndAccessSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
    }
    private void setupGoalsSettings() {
        EditText goalEdit = findViewById(R.id.edit_daily_goal_minutes);

        int goal = AppSettings.getDailyGoalMinutes(this);

        goalEdit.setText(String.valueOf(goal));

        goalEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                int v = 60;
                try {
                    String t = s.toString().trim();
                    if (!t.isEmpty()) v = Integer.parseInt(t);
                } catch (NumberFormatException ignored) {}

                if (v < 5) v = 5;
                if (v > 180) v = 180;
                AppSettings.setDailyGoalMinutes(getApplicationContext(), v);

                Log.d("GoalDebug", "Settings saved goal=" + AppSettings.getDailyGoalMinutes(SettingsActivity.this));

            }
        });
    }

    private void setupManagePracticesSettings() {
        findViewById(R.id.button_create_practice).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AddPracticeActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.button_edit_delete_practice).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, EditDeletePracticeActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.button_export_practices).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ExportPracticeActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.button_import_practices).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ImportPracticeActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.button_back_manage_practices).setOnClickListener(v -> showSubPage(null));
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (pendingDndEnable) {
            if (hasDndAccess()) {
                pendingDndEnable = false;
                AppSettings.setDndEnabled(this,true);
                if (switchDnd != null) switchDnd.setChecked(true);
            }
        } else {
            // keep UI in sync if user changed permission outside
            if (switchDnd != null) {
                boolean dndOn = AppSettings.isDndEnabled(this);
                switchDnd.setChecked(dndOn);
            }
        }
    }

    private void setupSystemSettings() {
     /**   findViewById(R.id.button_edit_vow).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, VowActivity.class);
            startActivity(intent);
        }); */

        findViewById(R.id.button_reset_practices).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.reset_practices_to_default)
                .setMessage(R.string.reset_practices_dialog_message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    PracticeRepository.getInstance().resetToDefault(this);
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        });

        findViewById(R.id.button_back_system).setOnClickListener(v -> showSubPage(null));
    }

    private void showSubPage(View subPageToShow) {
        mainMenu.setVisibility(subPageToShow == null ? View.VISIBLE : View.GONE);
        languageContainer.setVisibility(subPageToShow == languageContainer ? View.VISIBLE : View.GONE);
        practiceContainer.setVisibility(subPageToShow == practiceContainer ? View.VISIBLE : View.GONE);
        managePracticesContainer.setVisibility(subPageToShow == managePracticesContainer ? View.VISIBLE : View.GONE);
        systemContainer.setVisibility(subPageToShow == systemContainer ? View.VISIBLE : View.GONE);

        if (subPageToShow == null) {
            headerText.setText(R.string.einstellungen);
        } else if (subPageToShow == languageContainer) {
            headerText.setText(R.string.settings_language);
        } else if (subPageToShow == practiceContainer) {
            headerText.setText(R.string.settings_practice);
        } else if (subPageToShow == managePracticesContainer) {
            headerText.setText(R.string.manage_practices);
        } else if (subPageToShow == systemContainer) {
            headerText.setText(R.string.settings_system);
        }
    }

    private void recreateApp() {
        Intent intent = new Intent(this, MeditationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (goalsContainer != null && goalsContainer.getVisibility() == View.VISIBLE) {
            showMainSettingsPage();
        } else {
            super.onBackPressed();
        }
    }

}
