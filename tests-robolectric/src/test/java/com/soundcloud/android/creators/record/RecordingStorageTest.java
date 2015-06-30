package com.soundcloud.android.creators.record;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.Recording;
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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class RecordingStorageTest {
    private static final String[] DATA_PROJECTIONS = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
    private static final String[] DISPLAY_NAME_PROJECTIONS = {MediaStore.MediaColumns.DISPLAY_NAME};

    private RecordingStorage recordingStorage;
    @Mock private AccountOperations accountOperations;
    @Mock private AudioDurationHelper durationHelper;
    @Mock private Observer<List<Recording>> recordingsObserver;
    @Mock private Observer<Void> staleUploadsObserver;
    @Mock private Observer<Recording> uploadObserver;
    @Mock private ContentResolver contentResolver;
    @Mock private Uri streamUri;
    @Mock private Cursor nameCursor;
    @Mock private Cursor fileCursor;
    @Captor private ArgumentCaptor<List<Recording>> resultCaptor;
    @Captor private ArgumentCaptor<Recording> recordingCaptor;

    private File recordingDir = new File("recording-dir");
    private File uploadsDir = new File("uploads-dir");
    private File externalDir = new File("external-dir");

    @Before
    public void setUp() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(123L));
        recordingDir.mkdir();
        uploadsDir.mkdir();
        externalDir.mkdir();
        recordingStorage = new RecordingStorage(accountOperations, durationHelper);
    }

    @After
    public void tearDown() throws Exception {
        IOUtils.cleanDir(recordingDir);
        IOUtils.cleanDir(uploadsDir);
        IOUtils.cleanDir(externalDir);
    }

    @Test
    public void cleanupRecordingsReturnsEmptyResult() {
        recordingStorage.cleanupRecordings(recordingDir).subscribe(recordingsObserver);

        verify(recordingsObserver).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue()).toBeEmpty();
    }

    @Test
    public void cleanupRecordingsCleansUpAmplitudeFile() throws IOException {
        new File(recordingDir, "something.amp").createNewFile();

        recordingStorage.cleanupRecordings(recordingDir).subscribe(recordingsObserver);

        verify(recordingsObserver).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue()).toBeEmpty();
    }

    @Test
    public void cleanupRecordingsCleansUpUnplayableFile() throws IOException {
        new File(recordingDir, "something.wav").createNewFile();

        recordingStorage.cleanupRecordings(recordingDir).subscribe(recordingsObserver);

        verify(recordingsObserver).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue()).toBeEmpty();
    }

    @Test
    public void cleanupRecordingsReturnsPlayableWavRecording() throws IOException {
        final File file = new File(recordingDir, "something.wav");
        file.createNewFile();
        when(durationHelper.getDuration(file)).thenReturn(123);

        recordingStorage.cleanupRecordings(recordingDir).subscribe(recordingsObserver);

        verify(recordingsObserver).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue()).toNumber(1);
        expect(resultCaptor.getValue().get(0).audio_path).toEqual(file);
    }

    @Test
    public void cleanupRecordingsReturnsPlayableOggRecording() throws IOException {
        final File file = new File(recordingDir, "something.ogg");
        file.createNewFile();
        when(durationHelper.getDuration(file)).thenReturn(123);

        recordingStorage.cleanupRecordings(recordingDir).subscribe(recordingsObserver);

        verify(recordingsObserver).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue()).toNumber(1);
        expect(resultCaptor.getValue().get(0).audio_path).toEqual(file);
    }

    @Test
    public void cleanupRecordingsReturnsPlayableRecordingAndCleaningStats() throws IOException {
        new File(recordingDir, "something_playable.amp").createNewFile();
        new File(recordingDir, "something_else.amp").createNewFile();
        new File(recordingDir, "something_unplayable.wav").createNewFile();
        final File file = new File(recordingDir, "something_playable.ogg");
        file.createNewFile();
        when(durationHelper.getDuration(file)).thenReturn(123);

        recordingStorage.cleanupRecordings(recordingDir).subscribe(recordingsObserver);

        verify(recordingsObserver).onNext(resultCaptor.capture());
        expect(resultCaptor.getValue()).toNumber(1);
        expect(resultCaptor.getValue().get(0).audio_path).toEqual(file);
    }

    @Test
    public void deleteStaleUploadsCleansUpUploadsFolder() throws IOException {
        final File upload = new File(uploadsDir, "upload.wav");
        upload.createNewFile();

        recordingStorage.deleteStaleUploads(uploadsDir).subscribe(staleUploadsObserver);

        verify(staleUploadsObserver).onCompleted();
        expect(upload.exists()).toBeFalse();
    }

    @Test
    public void uploadFileUriDoesNotCopyToUploadsFolder() throws IOException {
        File file = new File(externalDir, "test.wav");
        file.createNewFile();
        when(durationHelper.getDuration(file)).thenReturn(123);

        recordingStorage.upload(uploadsDir, Uri.fromFile(file), "audio/wav", contentResolver).subscribe(uploadObserver);

        verify(uploadObserver).onNext(recordingCaptor.capture());
        Recording recording = recordingCaptor.getValue();
        expect(recording.getAbsolutePath()).toEqual(file.getAbsolutePath());
        expect(IOUtils.nullSafeListFiles(uploadsDir, null).length).toEqual(0);
    }

    @Test
    public void uploadStreamUriCopiesToUploadsFolder() throws IOException {
        Uri uri = Uri.parse("content://stream/content");
        setupStreamUri(uri, "test content");
        setupCursor(fileCursor, uri, DATA_PROJECTIONS, null);
        setupCursor(nameCursor, uri, DISPLAY_NAME_PROJECTIONS, "resolved.wav");

        recordingStorage.upload(uploadsDir, uri, "audio/wav", contentResolver).subscribe(uploadObserver);

        verify(uploadObserver).onNext(recordingCaptor.capture());
        Recording recording = recordingCaptor.getValue();
        expect(recording.getFile().getParentFile()).toEqual(uploadsDir);
        expect(recording.original_filename).toEqual("resolved.wav");
        expect(IOUtils.nullSafeListFiles(uploadsDir, null).length).toEqual(1);
    }

    @Test
    public void uploadResolvesToFileDoesNotCopyToUploadsFolder() throws IOException {
        Uri uri = Uri.parse("content://file/content");
        File file = new File(externalDir, "test.wav");
        file.createNewFile();
        setupCursor(fileCursor, uri, DATA_PROJECTIONS, file.getAbsolutePath());

        recordingStorage.upload(uploadsDir, uri, "audio/wav", contentResolver).subscribe(uploadObserver);

        verify(uploadObserver).onNext(recordingCaptor.capture());
        Recording recording = recordingCaptor.getValue();
        expect(recording.getAbsolutePath()).toEqual(file.getAbsolutePath());
        expect(recording.original_filename).toEqual("test.wav");
        expect(IOUtils.nullSafeListFiles(uploadsDir, null).length).toEqual(0);
    }

    private void setupStreamUri(Uri uri, String content) throws IOException {
        File file = new File(externalDir, "stream.wav");
        IOUtils.writeFileFromString(file, content);

        InputStream is = new FileInputStream(file);
        when(contentResolver.openInputStream(uri)).thenReturn(is);
    }

    private void setupCursor(Cursor cursor, Uri uri, String[] projections, String value) {
        when(contentResolver.query(uri, projections, null, null, null)).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndex(anyString())).thenReturn(0);
        when(cursor.getString(0)).thenReturn(value);
    }
}
