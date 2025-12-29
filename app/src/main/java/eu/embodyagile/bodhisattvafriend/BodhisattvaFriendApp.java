package eu.embodyagile.bodhisattvafriend;

import android.app.Application;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;

public class BodhisattvaFriendApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // WICHTIG: Practices aus JSON laden (nur einmal)
        PracticeRepository
                .getInstance()
                .init(getApplicationContext());
    }
}