package com.soundcloud.android.associations;

import com.soundcloud.android.storage.SoundAssociationStorage;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class SoundAssociationOperations {

    private SoundAssociationStorage mSoundAssocStorage;

    @Inject
    public SoundAssociationOperations(SoundAssociationStorage soundAssocStorage) {
        mSoundAssocStorage = soundAssocStorage;
    }

    public Observable<List<Long>> getLikedTracksIds() {
        return mSoundAssocStorage.getTrackLikesAsIdsAsync();
    }
}
