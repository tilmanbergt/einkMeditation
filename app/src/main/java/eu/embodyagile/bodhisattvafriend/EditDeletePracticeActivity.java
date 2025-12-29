package eu.embodyagile.bodhisattvafriend;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.model.Practice;

public class EditDeletePracticeActivity extends BaseActivity {

    public static final String EXTRA_PRACTICE_ID = "extra_practice_id";
    private static final int EDIT_PRACTICE_REQUEST = 1;
    private static final int PRACTICES_PER_PAGE = 5;

    private LinearLayout practiceListContainer;
    private Button prevButton, nextButton;
    private TextView pageNumberText;

    private List<Practice> allPractices;
    private int currentPage = 0;
    private int totalPages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_delete_practice);

        practiceListContainer = findViewById(R.id.practice_list_container);
        prevButton = findViewById(R.id.button_prev_page);
        nextButton = findViewById(R.id.button_next_page);
        pageNumberText = findViewById(R.id.page_number_text);

        findViewById(R.id.button_back_to_settings).setOnClickListener(v -> finish());

        loadPractices();
        setupPagination();
        displayPage(currentPage);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_PRACTICE_REQUEST && resultCode == RESULT_OK) {
            // Practice was changed, reload and refresh the list
            loadPractices();
            setupPagination();
            displayPage(currentPage);
        }
    }

    private void loadPractices() {
        allPractices = PracticeRepository.getInstance().getAllPractices();
        Collections.sort(allPractices, (p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
    }

    private void setupPagination() {
        totalPages = (int) Math.ceil((double) allPractices.size() / PRACTICES_PER_PAGE);
        if (currentPage >= totalPages && totalPages > 0) {
            currentPage = totalPages - 1;
        }

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
            TextView practiceView = (TextView) LayoutInflater.from(this).inflate(R.layout.item_practice_edit, practiceListContainer, false);

            practiceView.setText(practice.getName());
            practiceView.setOnClickListener(v -> {
                Intent intent = new Intent(EditDeletePracticeActivity.this, AddPracticeActivity.class);
                intent.putExtra(EXTRA_PRACTICE_ID, practice.getId());
                startActivityForResult(intent, EDIT_PRACTICE_REQUEST);
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
}
