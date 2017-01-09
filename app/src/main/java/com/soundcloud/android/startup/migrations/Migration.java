package com.soundcloud.android.startup.migrations;


import java.util.Comparator;

interface Migration {

    Comparator<Migration> APPLICABLE_VERSION_COMPARATOR = (lhs, rhs) -> lhs.getApplicableAppVersionCode() - rhs.getApplicableAppVersionCode();

    void applyMigration();

    int getApplicableAppVersionCode();
}
