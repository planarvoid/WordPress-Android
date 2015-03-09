package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.InsertResult;

public final class TestStorageResults {

    public static InsertResult successfulInsert() {
        return new InsertResult(1L);
    }

    public static ChangeResult successfulChange() {
        return new ChangeResult(1);
    }

    private TestStorageResults() {
        // no instances
    }
}
