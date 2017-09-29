package com.soundcloud.android.offline;

import com.soundcloud.android.commands.Command;

import javax.inject.Inject;

public class ClearOfflineContentCommand extends Command<Void, Boolean> {

    private final SecureFileStorage secureFileStorage;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final TrackOfflineStateProvider trackOfflineStateProvider;
    private final OfflineContentStorage offlineContentStorage;

    @Inject
    ClearOfflineContentCommand(SecureFileStorage secureFileStorage,
                               OfflineSettingsStorage offlineSettingsStorage,
                               TrackOfflineStateProvider trackOfflineStateProvider,
                               OfflineContentStorage offlineContentStorage) {
        this.secureFileStorage = secureFileStorage;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.trackOfflineStateProvider = trackOfflineStateProvider;
        this.offlineContentStorage = offlineContentStorage;
    }

    @Override
    public Boolean call(Void input) {
        if (offlineContentStorage.removeAllOfflineContent().blockingGet() == null) {
            trackOfflineStateProvider.clear();
            // Free space right now
            secureFileStorage.deleteAllTracks();
            offlineSettingsStorage.setHasOfflineContent(false);
            return true;
        }
        return false;
    }

}
