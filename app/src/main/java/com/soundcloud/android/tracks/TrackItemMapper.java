package com.soundcloud.android.tracks;

import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.Field.field;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Table;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class TrackItemMapper extends RxResultMapper<PropertySet> {

    private static final String SHARING_PRIVATE = "private";

    private static Object soundViewAs(String fieldName) {
        return field(Table.SoundView.field(fieldName)).as(fieldName);
    }

    public static final List<Object> BASE_TRACK_FIELDS =
            Collections.unmodifiableList(Arrays.asList(
                    soundViewAs(SoundView._ID),
                    soundViewAs(SoundView.TITLE),
                    soundViewAs(SoundView.USERNAME),
                    soundViewAs(SoundView.USER_ID),
                    soundViewAs(SoundView.SNIPPET_DURATION),
                    soundViewAs(SoundView.FULL_DURATION),
                    soundViewAs(SoundView.PLAYBACK_COUNT),
                    soundViewAs(SoundView.COMMENT_COUNT),
                    soundViewAs(SoundView.COMMENTABLE),
                    soundViewAs(SoundView.LIKES_COUNT),
                    soundViewAs(SoundView.REPOSTS_COUNT),
                    soundViewAs(SoundView.WAVEFORM_URL),
                    soundViewAs(SoundView.STREAM_URL),
                    soundViewAs(SoundView.ARTWORK_URL),
                    soundViewAs(SoundView.POLICIES_MONETIZABLE),
                    soundViewAs(SoundView.POLICIES_BLOCKED),
                    soundViewAs(SoundView.POLICIES_SNIPPED),
                    soundViewAs(SoundView.POLICIES_POLICY),
                    soundViewAs(SoundView.POLICIES_SUB_HIGH_TIER),
                    soundViewAs(SoundView.POLICIES_MONETIZATION_MODEL),
                    soundViewAs(SoundView.PERMALINK_URL),
                    soundViewAs(SoundView.SHARING),
                    soundViewAs(SoundView.CREATED_AT),
                    soundViewAs(SoundView.OFFLINE_DOWNLOADED_AT),
                    soundViewAs(SoundView.OFFLINE_REMOVED_AT)));

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

        final Urn urn = readTrackUrn(cursorReader);
        propertySet.put(TrackProperty.URN, urn);
        propertySet.put(PlayableProperty.TITLE, cursorReader.getString(SoundView.TITLE));
        propertySet.put(TrackProperty.SNIPPET_DURATION, cursorReader.getLong(SoundView.SNIPPET_DURATION));
        propertySet.put(TrackProperty.FULL_DURATION, cursorReader.getLong(SoundView.FULL_DURATION));
        propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(SoundView.PLAYBACK_COUNT));
        propertySet.put(TrackProperty.COMMENTS_COUNT, cursorReader.getInt(SoundView.COMMENT_COUNT));
        propertySet.put(TrackProperty.IS_COMMENTABLE, cursorReader.getBoolean(SoundView.COMMENTABLE));
        propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(SoundView.LIKES_COUNT));
        propertySet.put(PlayableProperty.REPOSTS_COUNT, cursorReader.getInt(SoundView.REPOSTS_COUNT));
        propertySet.put(TrackProperty.MONETIZABLE, cursorReader.getBoolean(SoundView.POLICIES_MONETIZABLE));
        propertySet.put(TrackProperty.BLOCKED, cursorReader.getBoolean(SoundView.POLICIES_BLOCKED));
        propertySet.put(TrackProperty.SNIPPED, cursorReader.getBoolean(SoundView.POLICIES_SNIPPED));
        propertySet.put(TrackProperty.SUB_HIGH_TIER, cursorReader.getBoolean(SoundView.POLICIES_SUB_HIGH_TIER));
        propertySet.put(TrackProperty.MONETIZATION_MODEL,
                        cursorReader.getString(SoundView.POLICIES_MONETIZATION_MODEL));
        propertySet.put(PlayableProperty.IS_USER_LIKE, cursorReader.getBoolean(SoundView.USER_LIKE));
        propertySet.put(PlayableProperty.PERMALINK_URL, cursorReader.getString(SoundView.PERMALINK_URL));
        propertySet.put(PlayableProperty.IS_USER_REPOST, cursorReader.getBoolean(SoundView.USER_REPOST));
        propertySet.put(PlayableProperty.IS_PRIVATE,
                        SHARING_PRIVATE.equalsIgnoreCase(cursorReader.getString(SoundView.SHARING)));
        propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(SoundView.CREATED_AT));
        propertySet.put(EntityProperty.IMAGE_URL_TEMPLATE, Optional.fromNullable(
                cursorReader.getString(SoundView.ARTWORK_URL)));

        putOptionalFields(cursorReader, propertySet);
        putOptionalOfflineSyncDates(cursorReader, propertySet);
        return propertySet;
    }

    private void putOptionalFields(CursorReader cursorReader, PropertySet propertySet) {
        final String policy = cursorReader.getString(SoundView.POLICIES_POLICY);
        if (policy != null) {
            propertySet.put(TrackProperty.POLICY, policy);
        }

        final String waveformUrl = cursorReader.getString(SoundView.WAVEFORM_URL);
        if (waveformUrl != null) {
            propertySet.put(TrackProperty.WAVEFORM_URL, waveformUrl);
        }

        // synced tracks that might not have a user if they haven't been lazily updated yet
        final String creator = cursorReader.getString(SoundView.USERNAME);
        propertySet.put(PlayableProperty.CREATOR_NAME, creator == null ? Strings.EMPTY : creator);
        final long creatorId = cursorReader.getLong(SoundView.USER_ID);
        propertySet.put(PlayableProperty.CREATOR_URN,
                        creatorId == Consts.NOT_SET ? Urn.NOT_SET : Urn.forUser(creatorId));
    }

    private void putOptionalOfflineSyncDates(CursorReader cursorReader, PropertySet propertySet) {
        final Date defaultDate = new Date(0);
        final Date removedAt = getDateOr(cursorReader, SoundView.OFFLINE_REMOVED_AT, defaultDate);
        final Date downloadedAt = getDateOr(cursorReader, SoundView.OFFLINE_DOWNLOADED_AT, defaultDate);

        if (isMostRecentDate(downloadedAt, removedAt)) {
            propertySet.put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);
        } else if (isMostRecentDate(removedAt, downloadedAt)) {
            propertySet.put(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
        }
    }

    private Date getDateOr(CursorReader cursorReader, String columnName, Date defaultDate) {
        if (cursorReader.isNotNull(columnName)) {
            return cursorReader.getDateFromTimestamp(columnName);
        }
        return defaultDate;
    }


    private boolean isMostRecentDate(Date dateToTest, Date... dates) {
        for (Date aDate : dates) {
            if (aDate.after(dateToTest) || aDate.equals(dateToTest)) {
                return false;
            }
        }
        return true;
    }

    private Urn readTrackUrn(CursorReader cursorReader) {
        return Urn.forTrack(cursorReader.getLong(SoundView._ID));
    }
}
