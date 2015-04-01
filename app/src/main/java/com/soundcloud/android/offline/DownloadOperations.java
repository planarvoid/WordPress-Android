package com.soundcloud.android.offline;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.DeleteOfflineTrackCommand;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Collection;

class DownloadOperations {
    private final StrictSSLHttpClient strictSSLHttpClient;
    private final SecureFileStorage fileStorage;
    private final DeleteOfflineTrackCommand deleteOfflineContent;
    private final PlayQueueManager playQueueManager;
    private final NetworkConnectionHelper connectionHelper;
    private final OfflineSettingsStorage offlineSettings;
    private final Predicate<Urn> isNotCurrentTrackFilter = new Predicate<Urn>() {
        @Override
        public boolean apply(Urn urn) {
            return !playQueueManager.isCurrentTrack(urn);
        }
    };
    private final Scheduler scheduler;

    @Inject
    public DownloadOperations(StrictSSLHttpClient httpClient,
                              SecureFileStorage fileStorage,
                              DeleteOfflineTrackCommand deleteOfflineContent,
                              PlayQueueManager playQueueManager,
                              NetworkConnectionHelper connectionHelper,
                              OfflineSettingsStorage offlineSettings,
                              @Named("HighPriority") Scheduler scheduler) {
        this.strictSSLHttpClient = httpClient;
        this.fileStorage = fileStorage;
        this.deleteOfflineContent = deleteOfflineContent;
        this.playQueueManager = playQueueManager;
        this.connectionHelper = connectionHelper;
        this.offlineSettings = offlineSettings;
        this.scheduler = scheduler;
    }

    boolean isValidNetwork() {
        return connectionHelper.isWifiConnected() || (!offlineSettings.isWifiOnlyEnabled() && connectionHelper.isNetworkConnected());
    }

    Observable<Collection<Urn>> removeOfflineTracks(Collection<Urn> requests) {
        return deleteOfflineContent
                .toObservable(Collections2.filter(requests, isNotCurrentTrackFilter))
                .subscribeOn(scheduler);
    }

    void deleteTrack(Urn urn) {
        try {
            fileStorage.deleteTrack(urn);
        } catch (EncryptionException e1) {
            // note, in this case, the file probably didn't exist in the first place, so we are in a clean state
            Log.e(OfflineContentService.TAG, "Failed to remove file", e1);
        }
    }

    public DownloadResult download(DownloadRequest request) {
        StrictSSLHttpClient.DownloadResponse response = null;

        try {
            response = strictSSLHttpClient.downloadFile(request.fileUrl);
            if (response.isUnavailable()) {
                return DownloadResult.unavailable(request);
            } else if (response.isFailure()) {
                return DownloadResult.failed(request);
            }
            saveFile(request, response);
            return DownloadResult.success(request);
        } catch (EncryptionException | IOException e) {
            deleteTrack(request.track);
            return DownloadResult.failed(request);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private void saveFile(DownloadRequest track, StrictSSLHttpClient.DownloadResponse response) throws IOException, EncryptionException {
        fileStorage.storeTrack(track.track, response.getInputStream());
        Log.d(OfflineContentService.TAG, "Track stored on device: " + track.track);
    }

}
