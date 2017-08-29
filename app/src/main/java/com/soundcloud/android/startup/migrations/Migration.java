package com.soundcloud.android.startup.migrations;


import java.util.Comparator;

interface Migration {

    Comparator<Migration> APPLICABLE_VERSION_COMPARATOR = (lhs, rhs) -> lhs.getApplicableAppVersionCode() - rhs.getApplicableAppVersionCode();

    void applyMigration();

    /**
     * This should be 1 greater than the version code in the manifest when you build a release
     */
    int getApplicableAppVersionCode();
}
