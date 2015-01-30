package com.soundcloud.android.likes;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.RxResultMapper;

import android.provider.BaseColumns;

public class LikedTrackMapper extends RxResultMapper<PropertySet> {

    private static final String SHARING_PRIVATE = "private";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

        propertySet.put(TrackProperty.URN, readSoundUrn(cursorReader));
        propertySet.put(PlayableProperty.TITLE, cursorReader.getString(TableColumns.Sounds.TITLE));
        propertySet.put(PlayableProperty.CREATOR_NAME, cursorReader.getString(TableColumns.Users.USERNAME));
        propertySet.put(PlayableProperty.DURATION, cursorReader.getInt(TableColumns.Sounds.DURATION));
        propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(TableColumns.Sounds.PLAYBACK_COUNT));
        propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.Sounds.LIKES_COUNT));
        propertySet.put(LikeProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.Likes.CREATED_AT));
        propertySet.put(PlayableProperty.IS_PRIVATE, SHARING_PRIVATE.equalsIgnoreCase(cursorReader.getString(TableColumns.Sounds.SHARING)));
        addOptionalOfflineSyncDates(cursorReader, propertySet);

        return propertySet;
    }

    private void addOptionalOfflineSyncDates(CursorReader cursorReader, PropertySet propertySet) {
        if (cursorReader.isNotNull(TableColumns.TrackDownloads.DOWNLOADED_AT)){
            propertySet.put(TrackProperty.OFFLINE_DOWNLOADED_AT, cursorReader.getDateFromTimestamp(TableColumns.TrackDownloads.DOWNLOADED_AT));
        }
        if (cursorReader.isNotNull(TableColumns.TrackDownloads.REMOVED_AT)){
            propertySet.put(TrackProperty.OFFLINE_REMOVED_AT, cursorReader.getDateFromTimestamp(TableColumns.TrackDownloads.REMOVED_AT));
        }
    }

    private Urn readSoundUrn(CursorReader cursorReader) {
        return Urn.forTrack(cursorReader.getInt(BaseColumns._ID));
    }
}
