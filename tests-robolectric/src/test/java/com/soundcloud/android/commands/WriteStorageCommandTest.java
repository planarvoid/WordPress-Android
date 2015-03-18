package com.soundcloud.android.commands;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestStorageResults;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Looper;

@RunWith(SoundCloudTestRunner.class)
public class WriteStorageCommandTest {

    @Mock private PropellerDatabase propeller;
    @Mock private Thread currentThread;

    @Test
    public void shouldReturnResultFromCallIfWriteSuccessful() throws Exception {
        DefaultWriteStorageCommand<String, InsertResult> successfulCommand =
                new DefaultWriteStorageCommand<String, InsertResult>(propeller, providerOf(currentThread)) {
            @Override
            protected InsertResult write(PropellerDatabase propeller, String input) {
                return TestStorageResults.successfulInsert();
            }
        };

        final InsertResult result = successfulCommand.call("input");
        expect(result.success()).toBeTrue();
    }

    @Test(expected = PropellerWriteException.class)
    public void shouldRethrowWriteExceptionIfWriteFailed() {
        DefaultWriteStorageCommand<String, InsertResult> failedCommand =
                new DefaultWriteStorageCommand<String, InsertResult>(propeller, providerOf(currentThread)) {
            @Override
            protected InsertResult write(PropellerDatabase propeller, String input) {
                return TestStorageResults.failedInsert();
            }
        };
        failedCommand.call("input");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIfWriteOperationPerformedOnMainThread() throws Exception {
        DefaultWriteStorageCommand<String, InsertResult> mainThreadCommand =
                new DefaultWriteStorageCommand<String, InsertResult>(propeller, providerOf(Looper.getMainLooper().getThread())) {
            @Override
            protected InsertResult write(PropellerDatabase propeller, String input) {
                return TestStorageResults.failedInsert();
            }
        };
        mainThreadCommand.call("input");
    }
}