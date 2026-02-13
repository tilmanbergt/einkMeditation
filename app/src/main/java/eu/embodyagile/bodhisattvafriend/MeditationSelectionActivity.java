package eu.embodyagile.bodhisattvafriend;

import static eu.embodyagile.bodhisattvafriend.MeditationActivity.EXTRA_PRACTICE_ID;
import static eu.embodyagile.bodhisattvafriend.MeditationActivity.EXTRA_PRE_INNER;
import static eu.embodyagile.bodhisattvafriend.MeditationActivity.EXTRA_PRE_TIME;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.helper.LocaleHelper;
import eu.embodyagile.bodhisattvafriend.logic.InnerCondition;
import eu.embodyagile.bodhisattvafriend.logic.QuickCheckAnswers;
import eu.embodyagile.bodhisattvafriend.logic.TimeAvailable;
import eu.embodyagile.bodhisattvafriend.model.Practice;

public class MeditationSelectionActivity extends BaseActivity {

    private LinearLayout listLayout;
    private TextView hintView;
    private InnerCondition preInnerCondition;
    private TimeAvailable preTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meditation_selection);
        String practiceId = getIntent().getStringExtra(EXTRA_PRACTICE_ID);
        String preTimeStr = getIntent().getStringExtra(EXTRA_PRE_TIME);
        if (preTimeStr != null) {
            try {
                preTime = TimeAvailable.valueOf(preTimeStr);
            } catch (IllegalArgumentException ignored) {
            }
        } else Log.d("MeditationSelectionActivity", "no time");


        String preInnerStr = getIntent().getStringExtra(EXTRA_PRE_INNER);
        if (preInnerStr != null) {
            try {
                preInnerCondition = InnerCondition.valueOf(preInnerStr);
            } catch (IllegalArgumentException ignored) {
            }
        } else {
            Log.d("MeditationSelectionActivity", "no inner condition");

        }
        // Sicherstellen, dass practices.json geladen ist
        //PracticeRepository.getInstance().init(getApplicationContext());

        listLayout = findViewById(R.id.layout_selection_list);
        hintView = findViewById(R.id.hintText);
        populateList();
    }

    private void populateList() {
        // Alle bekannten IDs aus dem Recommender
        PracticeRepository repo = PracticeRepository.getInstance();
        List<Practice> practices;
        if (preInnerCondition == null && preTime == null) {
            practices = repo.getPracticesForManualSelection();
            Log.d("MeditationSelectionActivity", "manualSelectionList");
        } else {
            practices = PracticeRepository.recommendPractices(new QuickCheckAnswers(preInnerCondition, preTime));
            Log.d("MeditationSelectionActivity", "manualSelectionList");
        }


        //List<String> ids = PracticeRecommender.getAllPracticeIds();

        if (practices == null || practices.isEmpty()) {
            if (hintView != null) {
                hintView.setText(getString(R.string.keine_praxen_definiert));
            }
            return;
        }

        boolean anyAdded = false;


        LayoutInflater inflater = LayoutInflater.from(this);

        for (Practice p : practices) {

            anyAdded = true;

            // 1) TextView aus XML mit Style inflaten
            TextView tv = (TextView) inflater.inflate(
                    R.layout.item_practice_selection,
                    listLayout,   // parent für LayoutParams
                    false         // noch nicht automatisch anhängen
            );

            // 2) Text setzen
            String label = p.getName();
            tv.setText(label);
            tv.setPadding(6, 6, 6, 6);

            // 3) Klick-Handler wie gehabt
            tv.setOnClickListener(v -> openMeditation(p));

            // 4) Dem Layout hinzufügen
            listLayout.addView(tv);
        }


        if (!anyAdded && hintView != null) {
            hintView.setText(getString(R.string.no_practices_found));
        }
    }

    private void openMeditation(Practice practice) {
        Intent intent = new Intent(MeditationSelectionActivity.this, MeditationActivity.class);
           intent.putExtra(EXTRA_PRACTICE_ID, practice.getId());
        startActivity(intent);
    }


}
