package eu.embodyagile.bodhisattvafriend;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.helper.LocaleHelper;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PracticeRepository
                .getInstance()
                .init(this);   // <-- WICHTIG, jedes Mal aufrufen für sprachewechsel


        TextView quickCheckButton = findViewById(R.id.button_quick_check);
        quickCheckButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QuickCheckActivity.class);
            startActivity(intent);
        });

        TextView chooseMeditationButton = findViewById(R.id.button_choose_meditation);
        chooseMeditationButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MeditationSelectionActivity.class);
            startActivity(intent);
        });

        TextView historyButton = findViewById(R.id.button_history);
        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        TextView btnSettings = findViewById(R.id.button_settings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });



    }

}
