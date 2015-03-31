package com.soundcloud.android.offline;

import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.DeletePendingRemovalCommand;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;

class DownloadOperations {
    private final StrictSSLHttpClient strictSSLHttpClient;
    private final SecureFileStorage fileStorage;
    private final DeletePendingRemovalCommand deleteOfflineContent;
    private final PlayQueueManager playQueueManager;
    private final NetworkConnectionHelper connectionHelper;
    private final OfflineSettingsStorage offlineSettings;
    private final Scheduler scheduler;

    @Inject
    public DownloadOperations(StrictSSLHttpClient httpClient,
                              SecureFileStorage fileStorage,
                              DeletePendingRemovalCommand deleteOfflineContent,
                              PlayQueueManager playQueueManager,
                              NetworkConnectionHelper connectionHelper,
                              OfflineSettingsStorage offlineSettings,
                              @Named("Storage") Scheduler scheduler) {
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

    Observable<List<Urn>> deletePendingRemovals() {
        return deleteOfflineContent
                .toObservable(playQueueManager.getCurrentTrackUrn())
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

    public DownloadResult download(DownloadRequest track) {
        StrictSSLHttpClient.DownloadResponse response = null;

        try {
            response = strictSSLHttpClient.downloadFile(track.fileUrl);
            if (response.isUnavailable()) {
                return DownloadResult.unavailable(track.urn);
            } else if (response.isFailure()) {
                return DownloadResult.failed(track.urn);
            }
            saveFile(track, response);
            return DownloadResult.success(track.urn);
        } catch (EncryptionException | IOException e) {
            deleteTrack(track.urn);
            return DownloadResult.failed(track.urn);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private void saveFile(DownloadRequest track, StrictSSLHttpClient.DownloadResponse response) throws IOException, EncryptionException {
        fileStorage.storeTrack(track.urn, response.getInputStream());
        Log.d(OfflineContentService.TAG, "Track stored on device: " + track.urn);
    }

}
