package eu.embodyagile.bodhisattvafriend;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.model.Practice;

public class AddPracticeActivity extends BaseActivity {

    private View detailsContainer, instructionsContainer;
    private EditText nameEdit, descriptionEdit, instructionEdit, durationsEdit;
    private Button deleteButton, saveDetailsButton;
    private TextView headerText, errorTextDetails, errorTextInstructions;

    private Practice existingPractice = null;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_practice);

        detailsContainer = findViewById(R.id.details_container);
        instructionsContainer = findViewById(R.id.instructions_container);

        nameEdit = findViewById(R.id.edit_practice_name);
        descriptionEdit = findViewById(R.id.edit_practice_description);
        instructionEdit = findViewById(R.id.edit_practice_instruction);
        durationsEdit = findViewById(R.id.edit_practice_durations);
        deleteButton = findViewById(R.id.button_delete_practice);
        saveDetailsButton = findViewById(R.id.button_save_details);
        headerText = findViewById(R.id.add_practice_header);
        errorTextDetails = findViewById(R.id.error_text_details);
        errorTextInstructions = findViewById(R.id.error_text_instructions);

        // Page 1 buttons
        findViewById(R.id.button_next).setOnClickListener(v -> {
            if (validateDetailsPage()) {
                showPage(instructionsContainer);
            }
        });
        findViewById(R.id.button_cancel_details).setOnClickListener(v -> finish());
        saveDetailsButton.setOnClickListener(v -> savePractice());

        // Page 2 buttons
        findViewById(R.id.button_back_instructions).setOnClickListener(v -> showPage(detailsContainer));
        findViewById(R.id.button_cancel_instructions).setOnClickListener(v -> finish());
        findViewById(R.id.button_save_practice).setOnClickListener(v -> savePractice());
        deleteButton.setOnClickListener(v -> deletePractice());

        String practiceId = getIntent().getStringExtra(EditDeletePracticeActivity.EXTRA_PRACTICE_ID);
        if (practiceId != null && !practiceId.isEmpty()) {
            isEditMode = true;
            existingPractice = PracticeRepository.getInstance().getPracticeById(practiceId);
        }

        if (isEditMode && existingPractice != null) {
            headerText.setText(R.string.edit_practice_header);
            nameEdit.setText(existingPractice.getName());
            descriptionEdit.setText(existingPractice.getShortDescription());
            instructionEdit.setText(existingPractice.getInstructionText());
            String durations = existingPractice.getDefaultDurationsMinutes().stream().map(String::valueOf).collect(Collectors.joining(","));
            durationsEdit.setText(durations);
            deleteButton.setVisibility(View.VISIBLE);
            saveDetailsButton.setVisibility(View.VISIBLE);
        }

        showPage(detailsContainer);
    }

    private void showPage(View pageToShow) {
        detailsContainer.setVisibility(pageToShow == detailsContainer ? View.VISIBLE : View.GONE);
        instructionsContainer.setVisibility(pageToShow == instructionsContainer ? View.VISIBLE : View.GONE);
        // Clear previous errors when switching pages
        if (pageToShow == instructionsContainer) {
            errorTextDetails.setVisibility(View.GONE);
        }
    }

    private boolean validateDetailsPage() {
        if (nameEdit.getText().toString().trim().isEmpty() || 
            descriptionEdit.getText().toString().trim().isEmpty() || 
            durationsEdit.getText().toString().trim().isEmpty()) {
            errorTextDetails.setText(R.string.toast_fill_out_all_fields);
            errorTextDetails.setVisibility(View.VISIBLE);
            return false;
        }
        try {
            Arrays.stream(durationsEdit.getText().toString().trim().split(",")).map(String::trim).map(Integer::parseInt).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            errorTextDetails.setText(R.string.toast_invalid_duration_format);
            errorTextDetails.setVisibility(View.VISIBLE);
            return false;
        }
        errorTextDetails.setVisibility(View.GONE);
        return true;
    }

    private void savePractice() {
        if (!validateDetailsPage()) {
            // Stay on the details page if validation fails
            showPage(detailsContainer);
            return;
        }

        String name = nameEdit.getText().toString().trim();
        String description = descriptionEdit.getText().toString().trim();
        String instruction = instructionEdit.getText().toString().trim();
        String durationsStr = durationsEdit.getText().toString().trim();

        List<Integer> durations = Arrays.stream(durationsStr.split(","))
            .map(String::trim)
            .map(Integer::parseInt)
            .collect(Collectors.toList());

        if (isEditMode) {
            existingPractice.setName(name);
            existingPractice.setShortDescription(description);
            existingPractice.setInstructionText(instruction);
            existingPractice.setDefaultDurationsMinutes(durations);
            PracticeRepository.getInstance().updatePractice(existingPractice, this);
        } else {
            Practice practice = new Practice(
                "custom_" + UUID.randomUUID().toString(),
                name,
                description,
                instruction,
                durations
            );
            PracticeRepository.getInstance().addPractice(practice, this);
        }

        setResult(RESULT_OK);
        finish();
    }

    private void deletePractice() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_practice_title)
            .setMessage(R.string.dialog_delete_practice_message)
            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                PracticeRepository.getInstance().deletePractice(existingPractice, this);
                setResult(RESULT_OK);
                finish();
            })
            .setNegativeButton(android.R.string.no, null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
}
