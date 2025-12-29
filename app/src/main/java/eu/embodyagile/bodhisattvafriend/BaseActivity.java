package eu.embodyagile.bodhisattvafriend;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import eu.embodyagile.bodhisattvafriend.helper.LocaleHelper;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}
