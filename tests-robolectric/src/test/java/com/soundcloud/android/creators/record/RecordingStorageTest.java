package com.soundcloud.android.creators.record;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observer;

import java.io.File;
import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class RecordingStorageTest {

    private RecordingStorage recordingStorage;
    @Mock private AccountOperations accountOperations;
    @Mock private AudioDurationHelper durationHelper;
    @Mock private Observer<CleanupRecordingsResult> observer;
    @Captor private ArgumentCaptor<CleanupRecordingsResult> resultCaptor;

    private File recordingDir = new File("recording-dir");

    @Before
    public void setUp() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(123L));
        recordingDir.mkdir();
        recordingStorage = new RecordingStorage(accountOperations, durationHelper);
    }

    @After
    public void tearDown() throws Exception {
        IOUtils.cleanDir(recordingDir);
    }

    @Test
    public void cleanupRecordingsReturnsEmptyResult() {
        recordingStorage.cleanupRecordings(recordingDir).subscribe(observer);

        verify(observer).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue().amplitudeFilesRemoved).toEqual(0);
        expect(resultCaptor.getValue().invalidRecordingsRemoved).toEqual(0);
        expect(resultCaptor.getValue().unsavedRecordings).toBeEmpty();
    }

    @Test
    public void cleanupRecordingsCleansUpAmplitudeFile() throws IOException {
        new File(recordingDir, "something.amp").createNewFile();

        recordingStorage.cleanupRecordings(recordingDir).subscribe(observer);

        verify(observer).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue().amplitudeFilesRemoved).toEqual(1);
        expect(resultCaptor.getValue().invalidRecordingsRemoved).toEqual(0);
        expect(resultCaptor.getValue().unsavedRecordings).toBeEmpty();
    }

    @Test
    public void cleanupRecordingsCleansUpUnplayableFile() throws IOException {
        new File(recordingDir, "something.wav").createNewFile();

        recordingStorage.cleanupRecordings(recordingDir).subscribe(observer);

        verify(observer).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue().amplitudeFilesRemoved).toEqual(0);
        expect(resultCaptor.getValue().invalidRecordingsRemoved).toEqual(1);
        expect(resultCaptor.getValue().unsavedRecordings).toBeEmpty();
    }

    @Test
    public void cleanupRecordingsReturnsPlayableWavRecording() throws IOException {
        final File file = new File(recordingDir, "something.wav");
        file.createNewFile();
        when(durationHelper.getDuration(file)).thenReturn(123);

        recordingStorage.cleanupRecordings(recordingDir).subscribe(observer);

        verify(observer).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue().amplitudeFilesRemoved).toEqual(0);
        expect(resultCaptor.getValue().invalidRecordingsRemoved).toEqual(0);
        expect(resultCaptor.getValue().unsavedRecordings).toNumber(1);
        expect(resultCaptor.getValue().unsavedRecordings.get(0).audio_path).toEqual(file);
    }

    @Test
    public void cleanupRecordingsReturnsPlayableOggRecording() throws IOException {
        final File file = new File(recordingDir, "something.ogg");
        file.createNewFile();
        when(durationHelper.getDuration(file)).thenReturn(123);

        recordingStorage.cleanupRecordings(recordingDir).subscribe(observer);

        verify(observer).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue().amplitudeFilesRemoved).toEqual(0);
        expect(resultCaptor.getValue().invalidRecordingsRemoved).toEqual(0);
        expect(resultCaptor.getValue().unsavedRecordings).toNumber(1);
        expect(resultCaptor.getValue().unsavedRecordings.get(0).audio_path).toEqual(file);
    }

    @Test
    public void cleanupRecordingsReturnsPlayableRecordingAndCleaningStats() throws IOException {
        new File(recordingDir, "something_playable.amp").createNewFile();
        new File(recordingDir, "something_else.amp").createNewFile();
        new File(recordingDir, "something_unplayable.wav").createNewFile();

        final File file = new File(recordingDir, "something_playable.ogg");
        file.createNewFile();
        when(durationHelper.getDuration(file)).thenReturn(123);

        recordingStorage.cleanupRecordings(recordingDir).subscribe(observer);

        verify(observer).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue().amplitudeFilesRemoved).toEqual(1);
        expect(resultCaptor.getValue().invalidRecordingsRemoved).toEqual(1);
        expect(resultCaptor.getValue().unsavedRecordings).toNumber(1);
        expect(resultCaptor.getValue().unsavedRecordings.get(0).audio_path).toEqual(file);
    }
}