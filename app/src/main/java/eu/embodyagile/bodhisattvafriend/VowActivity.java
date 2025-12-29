package eu.embodyagile.bodhisattvafriend;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import eu.embodyagile.bodhisattvafriend.helper.LocaleHelper;
import eu.embodyagile.bodhisattvafriend.logic.VowManager;

public class VowActivity extends BaseActivity {

    private EditText editVow;
    private Button btnSave;
    private Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vow);

        editVow = findViewById(R.id.edit_vow_text);
        btnSave = findViewById(R.id.button_vow_save);
        btnCancel = findViewById(R.id.button_vow_cancel);

        String currentVow = VowManager.getCoreVow(this);
        if (!TextUtils.isEmpty(currentVow)) {
            editVow.setText(currentVow);
        }

        btnSave.setOnClickListener(v -> {
            String vowText = editVow.getText().toString().trim();
            VowManager.setCoreVow(this, vowText);
            goHome();
        });

        btnCancel.setOnClickListener(v -> {
            goHome();
        });
    }

    private void goHome() {
        Intent intent = new Intent(VowActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

}
