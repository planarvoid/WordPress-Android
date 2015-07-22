package com.soundcloud.android.playlists;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadStateMapper;
import com.soundcloud.android.policies.PolicyMapper;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

class PlaylistTrackItemMapper extends RxResultMapper<PropertySet> {
    private static final String SHARING_PRIVATE = "private";
    private final DownloadStateMapper downloadStateMapper;
    private final PolicyMapper policyMapper;

    PlaylistTrackItemMapper() {
        downloadStateMapper = new DownloadStateMapper();
        policyMapper = new PolicyMapper();
    }

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

        propertySet.put(TrackProperty.URN, readTrackUrn(cursorReader));
        propertySet.put(PlayableProperty.TITLE, cursorReader.getString(TableColumns.Sounds.TITLE));
        propertySet.put(PlayableProperty.DURATION, cursorReader.getLong(TableColumns.Sounds.DURATION));
        propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(TableColumns.Sounds.PLAYBACK_COUNT));
        propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.Sounds.LIKES_COUNT));
        propertySet.put(PlayableProperty.IS_PRIVATE, SHARING_PRIVATE.equalsIgnoreCase(cursorReader.getString(TableColumns.Sounds.SHARING)));
        propertySet.put(PlayableProperty.CREATOR_NAME, cursorReader.getString(TableColumns.Users.USERNAME));
        propertySet.put(PlayableProperty.CREATOR_URN, Urn.forUser(cursorReader.getLong(TableColumns.Sounds.USER_ID)));
        propertySet.update(downloadStateMapper.map(cursorReader));
        propertySet.update(policyMapper.map(cursorReader));
        return propertySet;
    }

    private Urn readTrackUrn(CursorReader cursorReader) {
        return Urn.forTrack(cursorReader.getLong(TableColumns.Sounds._ID));
    }
}
