package com.soundcloud.android.creators.record;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.io.File;

@RunWith(SoundCloudTestRunner.class)
public class RecordingOperationsTest {

    private RecordingOperations recordingOperations;

    @Mock private RecordingStorage recordingStorage;

    private File recordingDir = new File("recor-dir");

    @Before
    public void setUp() throws Exception {
        recordingOperations = new RecordingOperations(Schedulers.immediate(), recordingStorage);
    }

    @Test
    public void getLastUnsavedRecordingEmitsUnsavedRecordingFromStorage() throws Exception {
        final CleanupRecordingsResult result = new CleanupRecordingsResult(null, 0, 0);

        when(recordingStorage.cleanupRecordings(recordingDir)).thenReturn(Observable.just(result));

        TestObserver<CleanupRecordingsResult> recordingObserver = new TestObserver<>();
        recordingOperations.cleanupRecordings(recordingDir).subscribe(recordingObserver);
        expect(recordingObserver.getOnNextEvents()).toContainExactly(result);
    }
}