package com.soundcloud.android.offline;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.soundcloud.android.ApplicationModule;
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

    enum ConnectionState {
        DISCONNECTED, CONNECTED, NOT_ALLOWED
    }

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
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.strictSSLHttpClient = httpClient;
        this.fileStorage = fileStorage;
        this.deleteOfflineContent = deleteOfflineContent;
        this.playQueueManager = playQueueManager;
        this.connectionHelper = connectionHelper;
        this.offlineSettings = offlineSettings;
        this.scheduler = scheduler;
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

    DownloadResult download(DownloadRequest request) {
        if (!fileStorage.isEnoughSpaceForTrack(request.duration)) {
            return DownloadResult.notEnoughSpace(request);
        }

        if (!isValidNetwork()) {
            return DownloadResult.connectionError(request, getConnectionState());
        }

        return downloadAndStore(request);
    }

    boolean isValidNetwork() {
        return getConnectionState() == ConnectionState.CONNECTED;
    }

    private DownloadResult downloadAndStore(DownloadRequest request) {
        StrictSSLHttpClient.DownloadResponse response = null;
        try {
            response = strictSSLHttpClient.downloadFile(request.fileUrl);
            if (response.isUnavailable()) {
                return DownloadResult.unavailable(request);
            }
            if (response.isFailure()) {
                return DownloadResult.error(request);
            }
            return saveFile(request, response);
        } catch (IOException ioException) {
            return DownloadResult.connectionError(request, getConnectionState());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private ConnectionState getConnectionState() {
        if (!connectionHelper.isNetworkConnected()) {
            return ConnectionState.DISCONNECTED;
        } else if (!connectionHelper.isWifiConnected() && offlineSettings.isWifiOnlyEnabled()) {
            return ConnectionState.NOT_ALLOWED;
        } else {
            return ConnectionState.CONNECTED;
        }
    }

    private DownloadResult saveFile(DownloadRequest request, StrictSSLHttpClient.DownloadResponse response) {
        try {
            fileStorage.storeTrack(request.track, response.getInputStream());
            Log.d(OfflineContentService.TAG, "Track stored on device: " + request.track);
            return DownloadResult.success(request);

        } catch (Exception exception) {
            Log.e(OfflineContentService.TAG, "Failed to save file: ", exception);
            deleteTrack(request.track);
            return DownloadResult.error(request);
        }
    }

}
