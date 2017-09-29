package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.MP3Helper.calculateFileSizeInBytes;
import static com.soundcloud.android.offline.StrictSSLHttpClient.TrackFileResponse;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.crypto.EncryptionInterruptedException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.StreamUrlBuilder;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;

class DownloadOperations {

    private final StrictSSLHttpClient strictSSLHttpClient;
    private final SecureFileStorage fileStorage;
    private final PlayQueueManager playQueueManager;
    private final StreamUrlBuilder urlBuilder;
    private final Scheduler scheduler;
    private final OfflineTrackAssetDownloader assetDownloader;
    private final DownloadConnectionHelper downloadConnectionHelper;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final TrackDownloadsStorage trackDownloadsStorage;

    @Inject
    DownloadOperations(StrictSSLHttpClient httpClient,
                       SecureFileStorage fileStorage,
                       PlayQueueManager playQueueManager,
                       StreamUrlBuilder urlBuilder,
                       @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                       OfflineTrackAssetDownloader assetDownloader,
                       DownloadConnectionHelper downloadConnectionHelper,
                       OfflineSettingsStorage offlineSettingsStorage,
                       TrackDownloadsStorage trackDownloadsStorage) {
        this.strictSSLHttpClient = httpClient;
        this.fileStorage = fileStorage;
        this.playQueueManager = playQueueManager;
        this.urlBuilder = urlBuilder;
        this.scheduler = scheduler;
        this.assetDownloader = assetDownloader;
        this.downloadConnectionHelper = downloadConnectionHelper;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.trackDownloadsStorage = trackDownloadsStorage;
    }

    boolean isConnectionValid() {
        return !downloadConnectionHelper.isNetworkDisconnected() && downloadConnectionHelper.isDownloadPermitted();
    }

    Single<List<Urn>> removeOfflineTracks(List<Urn> requests) {
        return Observable.fromIterable(requests)
                         .filter(urn -> !playQueueManager.isCurrentTrack(urn))
                         .flatMapSingle(this::deleteFromFileStorage)
                         .flatMapMaybe((urn) -> trackDownloadsStorage.deleteWithUrn(urn)
                                                                     .filter(count -> count > 0)
                                                                     .map(l -> urn))
                         .toList()
                         .subscribeOn(scheduler);

    }

    Single<Urn> deleteFromFileStorage(Urn urn) {
        return Single.fromCallable(() -> {
            fileStorage.deleteTrack(urn);
            return urn;
        });
    }

    void cancelCurrentDownload() {
        fileStorage.tryCancelRunningEncryption();
    }

    DownloadState download(DownloadRequest request, DownloadProgressListener listener) {
        if (!offlineSettingsStorage.isOfflineContentAccessible()) {
            return DownloadState.inaccessibleStorage(request);
        }

        if (!fileStorage.isEnoughMinimumSpace()) {
            return DownloadState.notEnoughMinimumSpace(request);
        }

        if (!fileStorage.isEnoughSpace(calculateFileSizeInBytes(request.getDuration()))) {
            return DownloadState.notEnoughSpace(request);
        }

        if (downloadConnectionHelper.isDownloadPermitted()) {
            return downloadAndStore(request, listener);
        } else if (downloadConnectionHelper.isNetworkDisconnected()) {
            return DownloadState.disconnectedNetworkError(request);
        } else {
            return DownloadState.invalidNetworkError(request);
        }
    }

    private DownloadState downloadAndStore(DownloadRequest request, DownloadProgressListener listener) {
        TrackFileResponse response = null;
        try {
            response = strictSSLHttpClient.getFileStream(urlBuilder.buildHttpsStreamUrl(request.getUrn()));

            if (response.isSuccess()) {
                saveTrack(request, response, listener);

                assetDownloader.fetchTrackArtwork(request);
                assetDownloader.fetchTrackWaveform(request.getUrn(), request.getWaveformUrl());

                return DownloadState.success(request);
            } else {
                return mapFailureToDownloadResult(request, response);
            }

        } catch (EncryptionInterruptedException interrupted) {
            return DownloadState.canceled(request);
        } catch (EncryptionException encryptionException) {
            return DownloadState.error(request);
        } catch (IOException ioException) {
            if (!offlineSettingsStorage.isOfflineContentAccessible()) {
                return DownloadState.inaccessibleStorage(request);
            }

            if (downloadConnectionHelper.isNetworkDisconnected()) {
                return DownloadState.disconnectedNetworkError(request);
            }

            return DownloadState.invalidNetworkError(request);
        } finally {
            IOUtils.close(response);
        }
    }

    private DownloadState mapFailureToDownloadResult(DownloadRequest request, TrackFileResponse response) {
        if (response.isUnavailable()) {
            return DownloadState.unavailable(request);
        }
        return DownloadState.error(request);
    }

    private void saveTrack(DownloadRequest request, TrackFileResponse response, final DownloadProgressListener listener)
            throws IOException, EncryptionException {

        fileStorage.storeTrack(request.getUrn(), response.getInputStream(), listener::onProgress);

        Log.d(OfflineContentService.TAG, "Track stored on device: " + request.getUrn());
    }

    interface DownloadProgressListener {
        void onProgress(long downloaded);
    }
}
