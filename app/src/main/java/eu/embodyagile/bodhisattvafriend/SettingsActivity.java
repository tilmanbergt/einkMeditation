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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.app.NotificationManager;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.List;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.helper.LocaleHelper;
import eu.embodyagile.bodhisattvafriend.helper.PagedListController;
import eu.embodyagile.bodhisattvafriend.history.MeditationInsightsRepository;
import eu.embodyagile.bodhisattvafriend.settings.AppSettings;
import android.net.Uri;
import android.content.ClipData;
import android.content.ClipboardManager;
public class SettingsActivity extends BaseActivity {

    private SwitchCompat switchDnd;
    private View settingsMainContainer;
    private View goalsContainer;

    private TextView buttonGoals;



    private boolean pendingDndEnable = false;

    private View mainMenu, languageContainer, practiceContainer, managePracticesContainer, systemContainer;
    private TextView headerText;

    private RadioGroup langGroup;
    private RadioButton langSystem, langDe, langEn;

    private PagedListController practicePager;
    private final List<View> practiceRows = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupFooter(FooterTab.SETTINGS);


        settingsMainContainer = findViewById(R.id.settings_main_menu);
        goalsContainer = findViewById(R.id.goalsContainer);

      //  buttonGoalsBack = findViewById(R.id.button_goals_back);
      //  buttonGoalsBack.setOnClickListener(v -> showSubPage(null));

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

        buttonGoals.setOnClickListener(v -> showSubPage(goalsContainer));


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

    //    findViewById(R.id.button_back_language).setOnClickListener(v -> showSubPage(null));
    }




    private void setupPracticeSettings() {
        LinearLayout host = findViewById(R.id.practice_settings_page_host);
        if (host == null) return;

        // Build row views once (bound + listeners attached)
        practiceRows.clear();

        // --- Timing ---
        practiceRows.add(createHeading(getString(R.string.settings_heading_timing)));

        EditText countdownEdit = createNumberRow(getString(R.string.pre_meditation_countdown_seconds));
        int countdown = AppSettings.getPremeditationCountdownSec(this);
        countdownEdit.setText(String.valueOf(countdown));
        bindNumberInstant(countdownEdit, value ->
                AppSettings.setPremeditationCountdownSec(getApplicationContext(), value)
        );

        // --- Feedback ---
        practiceRows.add(createHeading(getString(R.string.settings_heading_feedback)));

        SwitchCompat vib = createSwitchRow(getString(R.string.vibrate_on_start_end));
        vib.setChecked(AppSettings.isVibrationOnStartEndEnabled(this));
        vib.setOnCheckedChangeListener((btn, isChecked) ->
                AppSettings.setVibrationOnStartEndEnabled(this, isChecked)
        );

        SwitchCompat flash = createSwitchRow(getString(R.string.flash_on_start_end));
        flash.setChecked(AppSettings.isFlashOnStartEndEnabled(this));
        flash.setOnCheckedChangeListener((btn, isChecked) ->
                AppSettings.setFlashOnStartEndEnabled(this, isChecked)
        );

        // --- Focus ---
        practiceRows.add(createHeading(getString(R.string.settings_heading_focus)));

        SwitchCompat candle = createSwitchRow(getString(R.string.show_candle_timer));
        candle.setChecked(AppSettings.isCandleEnabled(this));
        candle.setOnCheckedChangeListener((btn, isChecked) ->
                AppSettings.setCandleEnabled(this, isChecked)
        );

        // DND (permission-aware)
        SwitchCompat dnd = createSwitchRow(getString(R.string.use_dnd_during_meditation));
        switchDnd = dnd; // IMPORTANT: used in onResume()
        dnd.setChecked(AppSettings.isDndEnabled(this));
        dnd.setOnCheckedChangeListener((btn, isChecked) -> {
            if (!isChecked) {
                pendingDndEnable = false;
                AppSettings.setDndEnabled(this, false);
                return;
            }

            if (hasDndAccess()) {
                AppSettings.setDndEnabled(this, true);
            } else {
                pendingDndEnable = true;
                dnd.setChecked(false); // keep UI honest until granted
                openDndAccessSettings();
                Toast.makeText(this, R.string.dnd_permission_needed, Toast.LENGTH_LONG).show();
            }
        });

        // Back button stays as-is
      //  findViewById(R.id.button_back_practice).setOnClickListener(v -> showSubPage(null));



        // --- Pager hookup (NOW INSIDE practiceContainer) ---
        View pagerControls = practiceContainer.findViewById(R.id.settings_pager_controls);
        TextView prev = practiceContainer.findViewById(R.id.pager_prev);
        TextView status = practiceContainer.findViewById(R.id.pager_status);
        TextView next = practiceContainer.findViewById(R.id.pager_next);

        if (practicePager == null) {
            practicePager = new PagedListController(host, pagerControls, prev, status, next);

            // Swipe: attach to HOST (better than whole container, fewer accidental flings)
            practicePager.attachSwipe(host);
        }

        practicePager.setRows(practiceRows);
        practicePager.computePagesAfterLayoutAndShow();
    }


    /** --- small helpers --- */

    private View createHeading(String text) {
        TextView h = (TextView) getLayoutInflater().inflate(R.layout.row_heading, null, false);
        h.setText(text);
        return h;
    }

    private SwitchCompat createSwitchRow(String label) {
        View row = getLayoutInflater().inflate(R.layout.row_switch, null, false);
        if (practicePager != null) {
            practicePager.installSwipeOn(row);
        }
        TextView tv = row.findViewById(R.id.row_label);
        SwitchCompat sw = row.findViewById(R.id.row_switch);
        tv.setText(label);
        practiceRows.add(row);
        return sw;
    }

    private EditText createNumberRow(String label) {
        View row = getLayoutInflater().inflate(R.layout.row_number, null, false);

        if (practicePager != null) {
            practicePager.installSwipeOn(row);

            View lbl = row.findViewById(R.id.row_label);
            if (lbl != null) practicePager.installSwipeOn(lbl);
            // Do NOT install on the EditText
        }

        TextView tv = row.findViewById(R.id.row_label);
        EditText ed = row.findViewById(R.id.row_edit);
        tv.setText(label);
        practiceRows.add(row);
        return ed;
    }



    private interface IntConsumer { void accept(int value); }

    private void bindNumberInstant(EditText edit, IntConsumer saver) {
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override public void afterTextChanged(Editable s) {
                int value = 0;
                try {
                    String t = s.toString().trim();
                    if (!t.isEmpty()) value = Integer.parseInt(t);
                } catch (NumberFormatException ignored) {}
                saver.accept(value);
            }
        });

        edit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                int value = 0;
                try {
                    String t = edit.getText().toString().trim();
                    if (!t.isEmpty()) value = Integer.parseInt(t);
                } catch (NumberFormatException ignored) {}
                saver.accept(value);
            }
        });
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
        EditText longTermGoalEdit = findViewById(R.id.edit_long_term_goal_minutes);

        // init
        int goal = AppSettings.getDailyGoalMinutes(this);
        goalEdit.setText(String.valueOf(goal));

        int longGoal = AppSettings.getLongTermGoalMinutes(this);
        longTermGoalEdit.setText(String.valueOf(longGoal));

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

        longTermGoalEdit.addTextChangedListener(new TextWatcher() {
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

                AppSettings.setLongTermGoalMinutes(getApplicationContext(), v);
                Log.d("GoalDebug", "Settings saved longTermGoal=" + AppSettings.getLongTermGoalMinutes(SettingsActivity.this));
            }
        });

        // Optional safety-net: clamp + write-back on focus loss (wie bei countdown)
        longTermGoalEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                int clamped = AppSettings.getLongTermGoalMinutes(SettingsActivity.this);
                longTermGoalEdit.setText(String.valueOf(clamped));
            }
        });

        goalEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                int clamped = AppSettings.getDailyGoalMinutes(SettingsActivity.this);
                goalEdit.setText(String.valueOf(clamped));
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

     //   findViewById(R.id.button_back_manage_practices).setOnClickListener(v -> showSubPage(null));
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
            if (switchDnd != null) {
                boolean dndOn = AppSettings.isDndEnabled(this);
                switchDnd.setChecked(dndOn);
            }
        }

        if (practiceContainer != null && practiceContainer.getVisibility() == View.VISIBLE && practicePager != null) {
            practicePager.computePages();
            practicePager.showPage(practicePager.getCurrentPage());
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

     //   findViewById(R.id.button_back_system).setOnClickListener(v -> showSubPage(null));
        TextView btnShowList = findViewById(R.id.button_show_last_practices);


        btnShowList.setOnClickListener(v -> {
            Intent intent = new Intent(this, SessionListActivity.class);
            startActivity(intent);
        });
        TextView tvInfo = findViewById(R.id.tv_system_info);
        if (tvInfo != null) {
            String info = "Author: Tilman Bergt\n" +
                    "App ID: " + BuildConfig.APPLICATION_ID + "\n" +
                            "Version: " + BuildConfig.VERSION_NAME + " (Code: " + BuildConfig.VERSION_CODE+ "/" +   "Build: " + BuildConfig.BUILD_TYPE  +  ")\n" +
                    "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " / " +
                            "Android: " + Build.VERSION.RELEASE;

            tvInfo.setText(info);

            // Optional: tap to copy
            tvInfo.setOnLongClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("System info", info));
                    Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        final String githubUrl = "https://github.com/tilmanbergt/einkMeditation";
        final String issuesUrl = githubUrl + "/issues";
        final String donateUrl = "https://buymeacoffee.com/nmx3st1m9l"; // or whatever you prefer

        View btnGitHub = findViewById(R.id.button_open_github);
        if (btnGitHub != null) {
            btnGitHub.setOnClickListener(v ->
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)))
            );
        }

        View btnBug = findViewById(R.id.button_report_bug);
        if (btnBug != null) {
            btnBug.setOnClickListener(v ->
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(issuesUrl)))
            );
        }

        View btnDonate = findViewById(R.id.button_donate);
        if (btnDonate != null) {
            btnDonate.setOnClickListener(v ->
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(donateUrl)))
            );
        }
    }
    public void showMainSettingsMenuFromFooter() {
        showSubPage(null); // your existing method that shows main menu
    }
    private void showSubPage(View subPageToShow) {
        mainMenu.setVisibility(subPageToShow == null ? View.VISIBLE : View.GONE);
        languageContainer.setVisibility(subPageToShow == languageContainer ? View.VISIBLE : View.GONE);
        practiceContainer.setVisibility(subPageToShow == practiceContainer ? View.VISIBLE : View.GONE);
        managePracticesContainer.setVisibility(subPageToShow == managePracticesContainer ? View.VISIBLE : View.GONE);
        systemContainer.setVisibility(subPageToShow == systemContainer ? View.VISIBLE : View.GONE);
        goalsContainer.setVisibility(subPageToShow == goalsContainer ? View.VISIBLE : View.GONE);

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
        } else if (subPageToShow == goalsContainer) {
            headerText.setText(R.string.goals); // add a string, or keep "Goals"
        }
        if (subPageToShow == practiceContainer && practicePager != null) {
            practicePager.computePagesAfterLayoutAndShow();
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
        if (mainMenu != null && mainMenu.getVisibility() != View.VISIBLE) {
            showSubPage(null);
        } else {
            super.onBackPressed();
        }
    }

}
