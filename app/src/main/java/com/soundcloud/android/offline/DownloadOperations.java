package com.soundcloud.android.offline;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.DeleteOfflineTrackCommand;
import com.soundcloud.android.playback.StreamUrlBuilder;
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
    private final StreamUrlBuilder urlBuilder;

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
                              StreamUrlBuilder urlBuilder,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.strictSSLHttpClient = httpClient;
        this.fileStorage = fileStorage;
        this.deleteOfflineContent = deleteOfflineContent;
        this.playQueueManager = playQueueManager;
        this.connectionHelper = connectionHelper;
        this.offlineSettings = offlineSettings;
        this.urlBuilder = urlBuilder;
        this.scheduler = scheduler;
    }

    Observable<Collection<Urn>> removeOfflineTracks(Collection<Urn> requests) {
        return deleteOfflineContent
                .toObservable(Collections2.filter(requests, isNotCurrentTrackFilter))
                .subscribeOn(scheduler);
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
            response = strictSSLHttpClient.downloadFile(urlBuilder.buildHttpsStreamUrl(request.track));
            if (response.isUnavailable()) {
                return DownloadResult.unavailable(request);
            }
            if (response.isFailure()) {
                return DownloadResult.error(request);
            }
            return saveFile(request, response);

        } catch (EncryptionException encryptionException) {
            return DownloadResult.error(request);
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

    private DownloadResult saveFile(DownloadRequest request, StrictSSLHttpClient.DownloadResponse response)
            throws IOException, EncryptionException {

        fileStorage.storeTrack(request.track, response.getInputStream());
        Log.d(OfflineContentService.TAG, "Track stored on device: " + request.track);
        return DownloadResult.success(request);
    }
}
