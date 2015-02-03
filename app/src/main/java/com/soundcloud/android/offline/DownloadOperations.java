package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
import com.soundcloud.android.offline.commands.StoreCompletedDownloadCommand;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class DownloadOperations {

    private final Func1<DownloadRequest, Observable<DownloadResult>> toDownloadResult = new Func1<DownloadRequest, Observable<DownloadResult>>() {
        @Override
        public Observable<DownloadResult> call(DownloadRequest request) {
            return processSequentialDownloadRequest(request);
        }
    };

    private final StrictSSLHttpClient strictSSLHttpClient;
    private final SecureFileStorage fileStorage;
    private final LoadPendingDownloadsCommand loadPendingDownloads;
    private final StoreCompletedDownloadCommand updateCompletedDownload;
    private final Scheduler scheduler;

    @Inject
    public DownloadOperations(StrictSSLHttpClient httpClient, SecureFileStorage fileStorage,
                              LoadPendingDownloadsCommand loadPending, StoreCompletedDownloadCommand updateDownload) {
        this(httpClient, fileStorage, loadPending, updateDownload, ScSchedulers.SEQUENTIAL_SYNCING_SCHEDULER);
    }

    @VisibleForTesting
    protected DownloadOperations(StrictSSLHttpClient httpClient, SecureFileStorage fileStorage,
                                 LoadPendingDownloadsCommand loadPending,
                                 StoreCompletedDownloadCommand updateCompleted,
                                 @Named("API") Scheduler scheduler) {
        this.strictSSLHttpClient = httpClient;
        this.fileStorage = fileStorage;
        this.updateCompletedDownload = updateCompleted;
        this.loadPendingDownloads = loadPending;
        this.scheduler = scheduler;
    }

    public Observable<List<DownloadRequest>> pendingDownloads() {
        return loadPendingDownloads.toObservable();
    }

    public Observable<DownloadResult> processDownloadRequests(List<DownloadRequest> requests) {
        return Observable.from(requests).flatMap(toDownloadResult);
    }

    private Observable<DownloadResult> processSequentialDownloadRequest(final DownloadRequest track) {
        return fetchSingleTrack(track)
                .doOnNext(updateCompletedDownload.toAction())
                .subscribeOn(scheduler);
    }

    private Observable<DownloadResult> fetchSingleTrack(final DownloadRequest track) {
        return Observable.create(new Observable.OnSubscribe<DownloadResult>() {
            @Override
            public void call(Subscriber<? super DownloadResult> subscriber) {
                Log.d(OfflineContentService.TAG, "Downloading track: " + track.urn + " from " + track.fileUrl);
                try {
                    subscriber.onNext(download(track));
                    subscriber.onCompleted();
                } catch (DownloadFailedException e) {
                    Log.e(OfflineContentService.TAG, "Failed to download file", e);
                    deleteTrack(track.urn);
                    subscriber.onError(e);
                }
            }
        });
    }

    private void deleteTrack(Urn urn) {
        try {
            fileStorage.deleteTrack(urn);
        } catch (EncryptionException e1) {
            // note, in this case, the file probably didn't exist in the first place, so we are in a clean state
            Log.e(OfflineContentService.TAG, "Failed to remove file", e1);
        }
    }

    private DownloadResult download(DownloadRequest track) throws DownloadFailedException {
        InputStream input = null;
        try {
            input = strictSSLHttpClient.downloadFile(track.fileUrl);
            fileStorage.storeTrack(track.urn, input);

            Log.d(OfflineContentService.TAG, "Track stored on device: " + track.urn);
            return new DownloadResult(true, track.urn);
        } catch (EncryptionException | IOException e) {
            throw new DownloadFailedException(track, e);
        } finally {
            IOUtils.close(input);
        }
    }

}
