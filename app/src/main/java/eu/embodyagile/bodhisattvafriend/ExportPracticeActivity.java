package eu.embodyagile.bodhisattvafriend;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.model.Practice;

public class ExportPracticeActivity extends BaseActivity {

    private static final int PRACTICES_PER_PAGE = 5;

    private LinearLayout practiceListContainer;
    private Button prevButton, nextButton, exportButton;
    private TextView pageNumberText;

    private List<Practice> allPractices;
    private final Map<String, Boolean> checkedPractices = new HashMap<>();
    private int currentPage = 0;
    private int totalPages = 0;

    private final ActivityResultLauncher<Intent> createDocumentLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    exportSelectedPractices(uri);
                }
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_practice);

        practiceListContainer = findViewById(R.id.export_practice_list_container);
        prevButton = findViewById(R.id.button_prev_page);
        nextButton = findViewById(R.id.button_next_page);
        pageNumberText = findViewById(R.id.page_number_text);
        exportButton = findViewById(R.id.button_export);

        findViewById(R.id.button_back_to_settings).setOnClickListener(v -> finish());
        exportButton.setOnClickListener(v -> createExportFile());

        loadPractices();
        setupPagination();
        displayPage(currentPage);
    }

    private void loadPractices() {
        allPractices = PracticeRepository.getInstance().getAllPractices();
        Collections.sort(allPractices, (p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
        for (Practice p : allPractices) {
            checkedPractices.put(p.getId(), false);
        }
    }

    private void setupPagination() {
        totalPages = (int) Math.ceil((double) allPractices.size() / PRACTICES_PER_PAGE);

        prevButton.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                displayPage(currentPage);
            }
        });

        nextButton.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                displayPage(currentPage);
            }
        });
    }

    private void displayPage(int page) {
        practiceListContainer.removeAllViews();

        int startIndex = page * PRACTICES_PER_PAGE;
        int endIndex = Math.min(startIndex + PRACTICES_PER_PAGE, allPractices.size());

        for (int i = startIndex; i < endIndex; i++) {
            final Practice practice = allPractices.get(i);
            View practiceView = LayoutInflater.from(this).inflate(R.layout.item_practice_export, practiceListContainer, false);

            CheckBox practiceCheckbox = practiceView.findViewById(R.id.practice_checkbox);
            practiceCheckbox.setText(practice.getName());
            practiceCheckbox.setChecked(checkedPractices.getOrDefault(practice.getId(), false));
            practiceCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                checkedPractices.put(practice.getId(), isChecked);
            });

            practiceListContainer.addView(practiceView);
        }

        if (totalPages == 0) {
            pageNumberText.setText("No practices found");
        } else {
            pageNumberText.setText("Page " + (page + 1) + " of " + totalPages);
        }
        prevButton.setEnabled(page > 0);
        nextButton.setEnabled(page < totalPages - 1);
    }

    private void createExportFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "practices.json");
        createDocumentLauncher.launch(intent);
    }

    private void exportSelectedPractices(Uri uri) {
        List<Practice> practicesToExport = new ArrayList<>();
        for (Practice p : allPractices) {
            if (checkedPractices.getOrDefault(p.getId(), false)) {
                practicesToExport.add(p);
            }
        }

        if (practicesToExport.isEmpty()) {
            Toast.makeText(this, "No practices selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // This is a simplified JSON export. A more robust solution would reuse the repository's logic.
            JSONObject root = new JSONObject();
            JSONArray practicesArray = new JSONArray();
            for (Practice practice : practicesToExport) {
                JSONObject practiceJson = new JSONObject();
                practiceJson.put("id", practice.getId());
                JSONObject nameJson = new JSONObject();
                nameJson.put("en", practice.getName()); // Assuming English for simplicity
                practiceJson.put("name", nameJson);
                practiceJson.put("shortDescription", practice.getShortDescription());
                practiceJson.put("instructionText", practice.getInstructionText());
                practiceJson.put("defaultDurationsMinutes", new JSONArray(practice.getDefaultDurationsMinutes()));
                practicesArray.put(practiceJson);
            }
            root.put("practices", practicesArray);

            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                os.write(root.toString(4).getBytes());
                Toast.makeText(this, "Practices exported", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Error exporting practices", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error creating JSON", Toast.LENGTH_SHORT).show();
        }
    }
}