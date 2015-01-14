package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class DownloadOperations {

    private final Action1<DownloadResult> updateDownloadCompleted = new Action1<DownloadResult>() {
        @Override
        public void call(DownloadResult result) {
            downloadsStorage.updateDownload(result);
        }
    };

    private final Func1<DownloadRequest, Observable<DownloadResult>> toDownloadResult = new Func1<DownloadRequest, Observable<DownloadResult>>() {
        @Override
        public Observable<DownloadResult> call(DownloadRequest request) {
            return processSequentialDownloadRequest(request);
        }
    };

    private final StrictSSLHttpClient strictSSLHttpClient;
    private final SecureFileStorage fileStorage;
    private final TrackDownloadsStorage downloadsStorage;
    private final Scheduler scheduler;

    @Inject
    public DownloadOperations(StrictSSLHttpClient httpClient, SecureFileStorage fileStorage,
                              TrackDownloadsStorage downloadsStorage) {
        this(httpClient, fileStorage, downloadsStorage, ScSchedulers.SEQUENTIAL_SYNCING_SCHEDULER);
    }

    @VisibleForTesting
    protected DownloadOperations(StrictSSLHttpClient httpClient, SecureFileStorage fileStorage,
                                 TrackDownloadsStorage downloadsStorage, @Named("API") Scheduler scheduler) {
        this.strictSSLHttpClient = httpClient;
        this.fileStorage = fileStorage;
        this.downloadsStorage = downloadsStorage;
        this.scheduler = scheduler;
    }

    public Observable<List<DownloadRequest>> pendingDownloads() {
        return downloadsStorage.getPendingDownloads();
    }

    public Observable<DownloadResult> processDownloadRequests(List<DownloadRequest> requests) {
        return Observable.from(requests).flatMap(toDownloadResult);
    }

    private Observable<DownloadResult> processSequentialDownloadRequest(final DownloadRequest track) {
        return fetchSingleTrack(track)
                .doOnNext(updateDownloadCompleted)
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
                } catch (IOException e) {
                    Log.e(OfflineContentService.TAG, "Failed to download file", e);
                    subscriber.onError(e);
                } catch (EncryptionException e) {
                    Log.e(OfflineContentService.TAG, "Failed to encrypt file", e);
                    subscriber.onError(e);
                }
            }
        });
    }

    private DownloadResult download(DownloadRequest track) throws IOException, EncryptionException {
        InputStream input = null;
        try {
            input = strictSSLHttpClient.downloadFile(track.fileUrl);
            fileStorage.storeTrack(track.urn, input);

            Log.d(OfflineContentService.TAG, "Track stored on device: " + track.urn);
            return new DownloadResult(true, track.urn);
        } finally {
            IOUtils.close(input);
        }
    }

}
