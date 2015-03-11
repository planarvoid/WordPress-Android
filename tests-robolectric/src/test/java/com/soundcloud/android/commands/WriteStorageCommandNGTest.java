package com.soundcloud.android.commands;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestStorageResults;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class WriteStorageCommandNGTest {

    private WriteStorageCommandNG<String, InsertResult> command = new WriteStorageCommandNG<String, InsertResult>() {
        @Override
        protected InsertResult store(String input) {
            return TestStorageResults.successfulInsert();
        }
    };

    private WriteStorageCommandNG<String, InsertResult> failedCommand = new WriteStorageCommandNG<String, InsertResult>() {
        @Override
        protected InsertResult store(String input) {
            return TestStorageResults.failedInsert();
        }
    };

    @Test
    public void shouldReturnResultFromCallIfWriteSuccessful() throws Exception {
        final InsertResult result = command.call("input");
        expect(result.success()).toBeTrue();
    }

    @Test(expected = PropellerWriteException.class)
    public void shouldRethrowWriteExceptionIfWriteFailed() {
        failedCommand.call("input");
    }
}