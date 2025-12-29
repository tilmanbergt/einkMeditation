package eu.embodyagile.bodhisattvafriend;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.model.Practice;

public class ImportPracticeActivity extends BaseActivity {

    private TextView statusText;

    private final ActivityResultLauncher<Intent> selectFileLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    processImport(uri);
                }
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_practice);

        statusText = findViewById(R.id.import_status_text);
        Button selectFileButton = findViewById(R.id.button_select_file);

        selectFileButton.setOnClickListener(v -> openFilePicker());
        findViewById(R.id.button_back_import).setOnClickListener(v -> finish());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        selectFileLauncher.launch(intent);
    }

    private void processImport(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            inputStream.close();

            JSONObject root = new JSONObject(stringBuilder.toString());
            JSONArray practicesArray = root.getJSONArray("practices");

            List<Practice> importedPractices = new ArrayList<>();
            for (int i = 0; i < practicesArray.length(); i++) {
                JSONObject practiceJson = practicesArray.getJSONObject(i);
                // This is a simplified parsing. A real implementation should handle this more robustly.
                String id = practiceJson.getString("id");
                String name = practiceJson.getJSONObject("name").getString("en"); // Assuming English for simplicity
                String description = practiceJson.getString("shortDescription");
                String instruction = practiceJson.getString("instructionText");
                JSONArray durationsArray = practiceJson.getJSONArray("defaultDurationsMinutes");
                List<Integer> durations = new ArrayList<>();
                for (int j = 0; j < durationsArray.length(); j++) {
                    durations.add(durationsArray.getInt(j));
                }
                importedPractices.add(new Practice(id, name, description, instruction, durations));
            }

            showConflictResolutionDialog(importedPractices);

        } catch (Exception e) {
            statusText.setText(R.string.import_failed);
        }
    }

    private void showConflictResolutionDialog(List<Practice> importedPractices) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.import_conflict_title)
            .setMessage(R.string.import_conflict_message)
            .setPositiveButton(R.string.overwrite_all, (dialog, which) -> {
                PracticeRepository.getInstance().importPractices(this, importedPractices, true);
                statusText.setText(R.string.import_successful);
            })
            .setNegativeButton(R.string.skip_all, (dialog, which) -> {
                PracticeRepository.getInstance().importPractices(this, importedPractices, false);
                statusText.setText(R.string.import_successful);
            })
            .setNeutralButton(R.string.abbrechen, null)
            .show();
    }
}
