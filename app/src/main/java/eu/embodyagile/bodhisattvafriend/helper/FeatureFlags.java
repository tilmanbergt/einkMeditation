package eu.embodyagile.bodhisattvafriend.helper;

import eu.embodyagile.bodhisattvafriend.BuildConfig;

public class FeatureFlags {
    private FeatureFlags() {}

    public static boolean changePracticeCompiledIn() {
        return BuildConfig.CHANGE_PRACTICE;
    }

    public static boolean importExportCompiledIn() {
        return BuildConfig.IMPORTEXPORT;
    }
    public static boolean practicemanagementCompiledIn() {
        return BuildConfig.PRACTICEMANAGEMENT;
    }

}
