package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
import com.soundcloud.android.offline.commands.StoreCompletedDownloadCommand;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class DownloadOperationsTest {

    @Mock private StrictSSLHttpClient httpClient;
    @Mock private SecureFileStorage fileStorage;
    @Mock private InputStream inputStream;
    @Mock private LoadPendingDownloadsCommand loadDownloadsCommand;
    @Mock private StoreCompletedDownloadCommand updateDownloadCommand;

    private DownloadOperations operations;
    private TestObserver<DownloadResult> observer;

    private final Urn trackUrn = Urn.forTrack(123L);
    private final String streamUrl = "http://stream1.url";
    private final DownloadRequest downloadRequest = new DownloadRequest(trackUrn, streamUrl);
    private final List<DownloadRequest> downloadRequests = Arrays.asList(this.downloadRequest);

    @Before
    public void setUp() throws Exception {
        operations = new DownloadOperations(httpClient, fileStorage, loadDownloadsCommand, updateDownloadCommand, Schedulers.immediate());
        observer = new TestObserver<>();

        when(loadDownloadsCommand.toObservable()).thenReturn(listOf1PendingDownload());
    }

    @Test
    public void pendingDownloadsGetsTrackToDownloadsFromDownloadStorage() throws IOException {
        final TestObserver<List<DownloadRequest>> observer = new TestObserver<>();
        operations.pendingDownloads().subscribe(observer);

        verify(loadDownloadsCommand).toObservable();
    }

    @Test
    public void processDownloadRequestCallsHttpClientWithGivenFileUrl() throws IOException {
        operations.processDownloadRequests(downloadRequests).subscribe(observer);

        verify(httpClient).downloadFile(downloadRequests.get(0).fileUrl);
    }

    @Test
    public void processDownloadRequestWritesToFileStorage() throws IOException, EncryptionException {
        when(httpClient.downloadFile(streamUrl)).thenReturn(inputStream);

        operations.processDownloadRequests(downloadRequests).subscribe(observer);

        InOrder inOrder = inOrder(fileStorage, inputStream);
        inOrder.verify(fileStorage).storeTrack(trackUrn, inputStream);
        inOrder.verify(inputStream).close();
    }

    @Test
    public void processDownloadUpdatedDownloadStorageAfterSuccessfulDownload() throws Exception {
        when(httpClient.downloadFile(streamUrl)).thenReturn(inputStream);

        operations.processDownloadRequests(downloadRequests).subscribe(observer);
        expect(updateDownloadCommand.getInput().getUrn()).toEqual(trackUrn);
    }

    @Test
    public void doesNotUpdateDownloadStateWhenDownloadFailed() throws Exception {
        when(httpClient.downloadFile(streamUrl)).thenThrow(new IOException("Test IOException"));

        operations.processDownloadRequests(downloadRequests).subscribe(observer);
        verifyZeroInteractions(updateDownloadCommand);
    }

    @Test
    public void doesNotUpdateDownloadStateWhenEncryptionFailed() throws Exception {
        when(httpClient.downloadFile(streamUrl)).thenReturn(inputStream);

        doThrow(new EncryptionException("Test EncryptionException", null))
                .when(fileStorage).storeTrack(trackUrn, inputStream);

        operations.processDownloadRequests(downloadRequests).subscribe(observer);
        verifyZeroInteractions(updateDownloadCommand);
        verify(inputStream).close();
    }

    @Test
    public void returnsDownloadExceptionWithTrackInfoWhenDownloadFailed() throws Exception {
        final IOException ioException = new IOException("Test IOException");
        when(httpClient.downloadFile(streamUrl)).thenThrow(ioException);
        operations.processDownloadRequests(downloadRequests).subscribe(observer);

        final List<Throwable> onErrorEvents = observer.getOnErrorEvents();
        expect(onErrorEvents.get(0)).toEqual(new DownloadFailedException(downloadRequest, ioException));
    }

    @Test
    public void returnsDownloadExceptionWhenEncryptionFailed() throws Exception {
        final EncryptionException encryptionException = new EncryptionException("Test EncryptionException", null);
        when(httpClient.downloadFile(streamUrl)).thenReturn(inputStream);
        doThrow(encryptionException)
                .when(fileStorage).storeTrack(trackUrn, inputStream);

        operations.processDownloadRequests(downloadRequests).subscribe(observer);

        final List<Throwable> onErrorEvents = observer.getOnErrorEvents();
        expect(onErrorEvents.get(0)).toEqual(new DownloadFailedException(downloadRequest, encryptionException));
    }

    @Test
    public void deletesFileFromFailedDownload() throws Exception {
        final IOException ioException = new IOException("Test IOException");
        when(httpClient.downloadFile(streamUrl)).thenThrow(ioException);

        operations.processDownloadRequests(downloadRequests).subscribe(observer);

        verify(fileStorage).deleteTrack(trackUrn);

    }

    @Test
    public void deletesFileFromFailedEncryption() throws Exception {
        final EncryptionException encryptionException = new EncryptionException("Test EncryptionException", null);
        when(httpClient.downloadFile(streamUrl)).thenReturn(inputStream);
        doThrow(encryptionException)
                .when(fileStorage).storeTrack(trackUrn, inputStream);

        operations.processDownloadRequests(downloadRequests).subscribe(observer);

        verify(fileStorage).deleteTrack(trackUrn);

    }

    private Observable<List<DownloadRequest>> listOf1PendingDownload() {
        return Observable.just(Arrays.asList(new DownloadRequest(trackUrn, streamUrl)));
    }

}
