package eu.embodyagile.bodhisattvafriend;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

import eu.embodyagile.bodhisattvafriend.helper.LocaleHelper;
import eu.embodyagile.bodhisattvafriend.settings.AppSettings;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    public enum FooterTab {HISTORY, PRACTICE, SETTINGS}
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        applyDisplaySettingsToRoot();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyDisplaySettingsToRoot();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        applyDisplaySettingsToRoot();
    }

    protected void applyDisplaySettingsToRoot() {
        ViewGroup content = findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;

        View root = content.getChildAt(0);
        if (root == null) return;

        if (AppSettings.isBackgroundFrameEnabled(this)) {
            root.setBackgroundResource(R.drawable.bg_frame_vintage);

            int left = getResources().getDimensionPixelSize(R.dimen.frame_padding_left);
            int top = getResources().getDimensionPixelSize(R.dimen.frame_padding_top);
            int right = getResources().getDimensionPixelSize(R.dimen.frame_padding_right);
            int bottom = getResources().getDimensionPixelSize(R.dimen.frame_padding_bottom);

            root.setPadding(left, top, right, bottom);
        } else {
            root.setBackground(null);
            int left = getResources().getDimensionPixelSize(R.dimen.frame_padding_left);
            int top = getResources().getDimensionPixelSize(R.dimen.frame_padding_top);
            int right = getResources().getDimensionPixelSize(R.dimen.frame_padding_right);
            int bottom = getResources().getDimensionPixelSize(R.dimen.frame_padding_bottom);

            root.setPadding(left, top, right, bottom);
        }
    }



    protected void setupFooter(FooterTab current) {
        Log.d("Footer", "Current: " + current);
        ImageButton history = findViewById(R.id.button_history);
        ImageButton practice = findViewById(R.id.button_andere_praxis);
        ImageButton settings = findViewById(R.id.button_settings);


        if (history != null) {
            Log.d("Footer", "Setup History Button");
            setFooterState(history, current == FooterTab.HISTORY);
            history.setOnClickListener(v -> {
                Log.d("Footer", "Go to History");
                startActivity(new Intent(this, HistoryActivity.class));
            });
        }

        if (practice != null) {
            Log.d("Footer", "Setup Practice Button");

            setFooterState(practice, current == FooterTab.PRACTICE);
            practice.setOnClickListener(v -> {
                Log.d("Footer", "Go to Settings");
                startActivity(new Intent(this, MeditationSetupActivity.class));
            });
        }

        if (settings != null) {
            Log.d("Footer", "Setup Settings Button");

            setFooterState(settings, current == FooterTab.SETTINGS);
            settings.setOnClickListener(v -> {
                Log.d("Footer", "Go to Settings");

                if (this instanceof SettingsActivity) {
                    ((SettingsActivity) this).showMainSettingsMenuFromFooter();
                } else {
                    Intent i = new Intent(this, SettingsActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                }
            });
        }
    }


    private void setFooterState(View button, boolean isCurrent) {

        //button.setEnabled(!isCurrent);
     //   button.setAlpha(isCurrent ? 1.0f : 0.5f);
      //  button.setVisibility(isCurrent ? View.GONE : View.VISIBLE);
        button.setBackgroundResource(
                isCurrent ? R.drawable.footer_active_underline : android.R.color.transparent
        );
    }
}
