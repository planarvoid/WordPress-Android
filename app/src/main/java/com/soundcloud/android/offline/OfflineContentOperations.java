package com.soundcloud.android.offline;

import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.TxnResult;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

public class OfflineContentOperations {

    private final SoundAssociationOperations associationOperations;
    private final TrackDownloadsStorage trackDownloadStorage;
    private final OfflineSettingsStorage settingsStorage;

    private final Func1<List<Urn>, Observable<TxnResult>> storeDownloadRequests = new Func1<List<Urn>, Observable<TxnResult>>() {
        @Override
        public Observable<TxnResult> call(List<Urn> tracks) {
            return trackDownloadStorage.filterAndStoreNewDownloadRequests(tracks);
        }
    };

    @Inject
    public OfflineContentOperations(TrackDownloadsStorage trackDownloadStorage,
                                    SoundAssociationOperations operations, OfflineSettingsStorage settingsStorage) {
        this.trackDownloadStorage = trackDownloadStorage;
        this.associationOperations = operations;
        this.settingsStorage = settingsStorage;
    }

    public Observable<TxnResult> updateOfflineLikes() {
        return associationOperations
                .getLikedTracks()
                .flatMap(storeDownloadRequests);
    }

    public void setLikesOfflineSync(boolean isEnabled) {
        settingsStorage.setLikesOfflineSync(isEnabled);
    }

    public boolean isLikesOfflineSyncEnabled() {
        return settingsStorage.isLikesOfflineSyncEnabled();
    }

    public Observable<Boolean> getSettingsStatus() {
        return settingsStorage.getLikesOfflineSyncChanged();
    }
}
