package com.soundcloud.android.commands;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestStorageResults;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class WriteStorageCommandTest {

    @Mock private PropellerDatabase propeller;

    private WriteStorageCommand<String, InsertResult> command = new WriteStorageCommand<String, InsertResult>(propeller) {
        @Override
        protected InsertResult write(PropellerDatabase propeller, String input) {
            return TestStorageResults.successfulInsert();
        }
    };

    private WriteStorageCommand<String, InsertResult> failedCommand = new WriteStorageCommand<String, InsertResult>(propeller) {
        @Override
        protected InsertResult write(PropellerDatabase propeller, String input) {
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