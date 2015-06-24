package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerWriteException;

public final class TestStorageResults {

    public static InsertResult successfulInsert() {
        return new InsertResult(1L);
    }

    public static InsertResult failedInsert() {
        return (InsertResult) new InsertResult(-1).fail(new PropellerWriteException("failure", new Exception()));
    }

    public static ChangeResult successfulChange() {
        return new ChangeResult(1);
    }

    private TestStorageResults() {
        // no instances
    }
}
