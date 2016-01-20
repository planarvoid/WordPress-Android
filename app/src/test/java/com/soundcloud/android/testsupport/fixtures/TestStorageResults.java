package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.TxnResult;

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

    public static TxnResult successfulTransaction() {
        return new TxnResult();
    }

    public static TxnResult failedTransaction() {
        return (TxnResult) new TxnResult().fail(new PropellerWriteException("failure", new Exception()));
    }

    private TestStorageResults() {
        // no instances
    }
}
