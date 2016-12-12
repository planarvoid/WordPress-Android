package com.soundcloud.android.tracks;

import static com.soundcloud.android.offline.OfflineStateMapper.getOfflineState;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Tables.TrackView;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import java.util.Date;

public final class TrackItemMapper extends RxResultMapper<PropertySet> {

    private static final String SHARING_PRIVATE = "private";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

        propertySet.put(TrackProperty.URN, Urn.forTrack(cursorReader.getLong(TrackView.ID.name())));
        propertySet.put(PlayableProperty.TITLE, cursorReader.getString(TrackView.TITLE.name()));
        propertySet.put(TrackProperty.SNIPPET_DURATION, cursorReader.getLong(TrackView.SNIPPET_DURATION.name()));
        propertySet.put(TrackProperty.FULL_DURATION, cursorReader.getLong(TrackView.FULL_DURATION.name()));
        propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(TrackView.PLAY_COUNT.name()));
        propertySet.put(TrackProperty.COMMENTS_COUNT, cursorReader.getInt(TrackView.COMMENTS_COUNT.name()));
        propertySet.put(TrackProperty.IS_COMMENTABLE, cursorReader.getBoolean(TrackView.IS_COMMENTABLE.name()));
        propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(TrackView.LIKES_COUNT.name()));
        propertySet.put(PlayableProperty.REPOSTS_COUNT, cursorReader.getInt(TrackView.REPOSTS_COUNT.name()));
        propertySet.put(TrackProperty.MONETIZABLE, cursorReader.getBoolean(TrackView.MONETIZABLE.name()));
        propertySet.put(TrackProperty.BLOCKED, cursorReader.getBoolean(TrackView.BLOCKED.name()));
        propertySet.put(TrackProperty.SNIPPED, cursorReader.getBoolean(TrackView.SNIPPED.name()));
        propertySet.put(TrackProperty.SUB_HIGH_TIER, cursorReader.getBoolean(TrackView.SUB_HIGH_TIER.name()));
        propertySet.put(TrackProperty.MONETIZATION_MODEL,
                        cursorReader.getString(TrackView.MONETIZATION_MODEL.name()));
        propertySet.put(PlayableProperty.IS_USER_LIKE, cursorReader.getBoolean(TrackView.IS_USER_LIKE.name()));
        propertySet.put(PlayableProperty.PERMALINK_URL, cursorReader.getString(TrackView.PERMALINK_URL.name()));
        propertySet.put(PlayableProperty.IS_USER_REPOST, cursorReader.getBoolean(TrackView.IS_USER_REPOST.name()));
        propertySet.put(PlayableProperty.IS_PRIVATE,
                        SHARING_PRIVATE.equalsIgnoreCase(cursorReader.getString(TrackView.SHARING.name())));
        propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TrackView.CREATED_AT.name()));
        propertySet.put(EntityProperty.IMAGE_URL_TEMPLATE, Optional.fromNullable(
                cursorReader.getString(TrackView.ARTWORK_URL.name())));

        putOptionalFields(cursorReader, propertySet);
        putOptionalOfflineSyncDates(cursorReader, propertySet);
        return propertySet;
    }

    private void putOptionalFields(CursorReader cursorReader, PropertySet propertySet) {
        final String policy = cursorReader.getString(TrackView.POLICY.name());
        if (policy != null) {
            propertySet.put(TrackProperty.POLICY, policy);
        }

        final String waveformUrl = cursorReader.getString(TrackView.WAVEFORM_URL.name());
        if (waveformUrl != null) {
            propertySet.put(TrackProperty.WAVEFORM_URL, waveformUrl);
        }

        // synced tracks that might not have a user if they haven't been lazily updated yet
        final String creator = cursorReader.getString(TrackView.USERNAME.name());
        propertySet.put(PlayableProperty.CREATOR_NAME, creator == null ? Strings.EMPTY : creator);
        final long creatorId = cursorReader.getLong(TrackView.USER_ID.name());
        propertySet.put(PlayableProperty.CREATOR_URN,
                        creatorId == Consts.NOT_SET ? Urn.NOT_SET : Urn.forUser(creatorId));
    }

    private void putOptionalOfflineSyncDates(CursorReader cursorReader, PropertySet propertySet) {
        final Date defaultDate = new Date(0);
        final Date removedAt = getDateOr(cursorReader, TrackView.OFFLINE_REMOVED_AT.name(), defaultDate);
        final Date downloadedAt = getDateOr(cursorReader, TrackView.OFFLINE_DOWNLOADED_AT.name(), defaultDate);
        final Date requestedAt = getDateOr(cursorReader, TrackView.OFFLINE_REQUESTED_AT.name(), defaultDate);
        final Date unavailableAt = getDateOr(cursorReader, TrackView.OFFLINE_UNAVAILABLE_AT.name(), defaultDate);
        OfflineState offlineState = getOfflineState(true, requestedAt, removedAt, downloadedAt, unavailableAt);
        propertySet.put(OfflineProperty.OFFLINE_STATE, offlineState);
    }

    private Date getDateOr(CursorReader cursorReader, String columnName, Date defaultDate) {
        if (cursorReader.isNotNull(columnName)) {
            return cursorReader.getDateFromTimestamp(columnName);
        }
        return defaultDate;
    }
}
