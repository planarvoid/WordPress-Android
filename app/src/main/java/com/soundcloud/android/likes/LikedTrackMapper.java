package com.soundcloud.android.likes;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineStateMapper;
import com.soundcloud.android.policies.PolicyMapper;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import android.provider.BaseColumns;

class LikedTrackMapper extends RxResultMapper<PropertySet> {

    private static final String SHARING_PRIVATE = "private";
    private final OfflineStateMapper offlineStateMapper;
    private final PolicyMapper policyMapper;

    LikedTrackMapper() {
        offlineStateMapper = new OfflineStateMapper();
        policyMapper = new PolicyMapper();
    }

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

        propertySet.put(TrackProperty.URN, readSoundUrn(cursorReader));
        propertySet.put(PlayableProperty.TITLE, cursorReader.getString(Tables.Sounds.TITLE));
        propertySet.put(PlayableProperty.CREATOR_NAME, cursorReader.getString(Tables.Users.USERNAME));
        propertySet.put(TrackProperty.SNIPPET_DURATION, cursorReader.getLong(Tables.Sounds.SNIPPET_DURATION));
        propertySet.put(TrackProperty.FULL_DURATION, cursorReader.getLong(Tables.Sounds.FULL_DURATION));
        propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(Tables.Sounds.PLAYBACK_COUNT));
        propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(Tables.Sounds.LIKES_COUNT));
        propertySet.put(LikeProperty.CREATED_AT, cursorReader.getDateFromTimestamp(Tables.Likes.CREATED_AT));
        propertySet.put(PlayableProperty.IS_PRIVATE,
                        SHARING_PRIVATE.equalsIgnoreCase(cursorReader.getString(Tables.Sounds.SHARING)));
        propertySet.update(offlineStateMapper.map(cursorReader));
        propertySet.update(policyMapper.map(cursorReader));
        return propertySet;
    }


    private Urn readSoundUrn(CursorReader cursorReader) {
        return Urn.forTrack(cursorReader.getLong(BaseColumns._ID));
    }
}
