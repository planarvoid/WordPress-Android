package com.soundcloud.android.migrations;


import java.util.Comparator;

interface Migration {


    public static final Comparator<Migration> APPLICABLE_VERSION_COMPARATOR = new Comparator<Migration>() {
        @Override
        public int compare(Migration lhs, Migration rhs) {
            return lhs.getApplicableAppVersionCode() - rhs.getApplicableAppVersionCode();
        }
    };

    void applyMigration();

    int getApplicableAppVersionCode();
}
