package com.soundcloud.android.tracks;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.RxResultMapper;

final class TrackItemMapper extends RxResultMapper<PropertySet> {

    private static final String SHARING_PRIVATE = "private";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

        propertySet.put(TrackProperty.URN, readTrackUrn(cursorReader));
        propertySet.put(PlayableProperty.TITLE, cursorReader.getString(TableColumns.SoundView.TITLE));
        propertySet.put(PlayableProperty.DURATION, cursorReader.getInt(TableColumns.SoundView.DURATION));
        propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(TableColumns.SoundView.PLAYBACK_COUNT));
        propertySet.put(TrackProperty.COMMENTS_COUNT, cursorReader.getInt(TableColumns.SoundView.COMMENT_COUNT));
        propertySet.put(TrackProperty.STREAM_URL, cursorReader.getString(TableColumns.SoundView.STREAM_URL));
        propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));
        propertySet.put(PlayableProperty.REPOSTS_COUNT, cursorReader.getInt(TableColumns.SoundView.REPOSTS_COUNT));
        propertySet.put(TrackProperty.MONETIZABLE, cursorReader.getBoolean(TableColumns.SoundView.MONETIZABLE));
        propertySet.put(PlayableProperty.IS_LIKED, cursorReader.getBoolean(TableColumns.SoundView.USER_LIKE));
        propertySet.put(PlayableProperty.PERMALINK_URL, cursorReader.getString(TableColumns.SoundView.PERMALINK_URL));
        propertySet.put(PlayableProperty.IS_REPOSTED, cursorReader.getBoolean(TableColumns.SoundView.USER_REPOST));
        propertySet.put(PlayableProperty.IS_PRIVATE, SHARING_PRIVATE.equalsIgnoreCase(cursorReader.getString(TableColumns.SoundView.SHARING)));
        propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.SoundView.CREATED_AT));

        putOptionalFields(cursorReader, propertySet);
        return propertySet;
    }

    private void putOptionalFields(CursorReader cursorReader, PropertySet propertySet) {
        final String policy = cursorReader.getString(TableColumns.SoundView.POLICY);
        final String waveformUrl = cursorReader.getString(TableColumns.SoundView.WAVEFORM_URL);

        if (policy != null) {
            propertySet.put(TrackProperty.POLICY, policy);
        }

        if (waveformUrl != null) {
            propertySet.put(TrackProperty.WAVEFORM_URL, waveformUrl);
        }

        // synced tracks that might not have a user if they haven't been lazily updated yet
        final String creator = cursorReader.getString(TableColumns.SoundView.USERNAME);
        propertySet.put(PlayableProperty.CREATOR_NAME, creator == null ? ScTextUtils.EMPTY_STRING : creator);
        final long creatorId = cursorReader.getLong(TableColumns.SoundView.USER_ID);
        propertySet.put(PlayableProperty.CREATOR_URN, creatorId == Consts.NOT_SET ? Urn.NOT_SET : Urn.forUser(creatorId));
    }

    private Urn readTrackUrn(CursorReader cursorReader) {
        return Urn.forTrack(cursorReader.getInt(TableColumns.SoundView._ID));
    }
}
