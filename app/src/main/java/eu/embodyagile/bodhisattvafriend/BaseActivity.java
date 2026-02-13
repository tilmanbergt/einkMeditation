package eu.embodyagile.bodhisattvafriend;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import eu.embodyagile.bodhisattvafriend.helper.LocaleHelper;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    public enum FooterTab {HISTORY, PRACTICE, SETTINGS}

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
