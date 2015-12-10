package com.soundcloud.android.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.fixtures.TestStorageResults;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WriteStorageCommandTest {

    @Mock private PropellerDatabase propeller;

    @Test
    public void shouldReturnResultFromCallIfWriteSuccessful() throws Exception {
        DefaultWriteStorageCommand<String, InsertResult> successfulCommand =
                new DefaultWriteStorageCommand<String, InsertResult>(propeller) {
            @Override
            protected InsertResult write(PropellerDatabase propeller, String input) {
                return TestStorageResults.successfulInsert();
            }
        };

        final InsertResult result = successfulCommand.call("input");
        assertThat(result.success()).isTrue();
    }

    @Test(expected = PropellerWriteException.class)
    public void shouldRethrowWriteExceptionIfWriteFailed() {
        DefaultWriteStorageCommand<String, InsertResult> failedCommand =
                new DefaultWriteStorageCommand<String, InsertResult>(propeller) {
            @Override
            protected InsertResult write(PropellerDatabase propeller, String input) {
                return TestStorageResults.failedInsert();
            }
        };
        failedCommand.call("input");
    }
}
