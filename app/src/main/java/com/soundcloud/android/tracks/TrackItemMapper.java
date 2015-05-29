package com.soundcloud.android.tracks;

import static com.soundcloud.android.storage.TableColumns.SoundView;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.RxResultMapper;

import java.util.Date;

final class TrackItemMapper extends RxResultMapper<PropertySet> {

    private static final String SHARING_PRIVATE = "private";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

        propertySet.put(TrackProperty.URN, readTrackUrn(cursorReader));
        propertySet.put(PlayableProperty.TITLE, cursorReader.getString(SoundView.TITLE));
        propertySet.put(PlayableProperty.DURATION, cursorReader.getLong(SoundView.DURATION));
        propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(SoundView.PLAYBACK_COUNT));
        propertySet.put(TrackProperty.COMMENTS_COUNT, cursorReader.getInt(SoundView.COMMENT_COUNT));
        propertySet.put(TrackProperty.STREAM_URL, cursorReader.getString(SoundView.STREAM_URL));
        propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(SoundView.LIKES_COUNT));
        propertySet.put(PlayableProperty.REPOSTS_COUNT, cursorReader.getInt(SoundView.REPOSTS_COUNT));
        propertySet.put(TrackProperty.MONETIZABLE, cursorReader.getBoolean(SoundView.POLICIES_MONETIZABLE));
        propertySet.put(PlayableProperty.IS_LIKED, cursorReader.getBoolean(SoundView.USER_LIKE));
        propertySet.put(PlayableProperty.PERMALINK_URL, cursorReader.getString(SoundView.PERMALINK_URL));
        propertySet.put(PlayableProperty.IS_REPOSTED, cursorReader.getBoolean(SoundView.USER_REPOST));
        propertySet.put(PlayableProperty.IS_PRIVATE, SHARING_PRIVATE.equalsIgnoreCase(cursorReader.getString(SoundView.SHARING)));
        propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(SoundView.CREATED_AT));

        putOptionalFields(cursorReader, propertySet);
        addOptionalOfflineSyncDates(cursorReader, propertySet);
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
        propertySet.put(PlayableProperty.CREATOR_NAME, creator == null ? ScTextUtils.EMPTY_STRING : creator);
        final long creatorId = cursorReader.getLong(SoundView.USER_ID);
        propertySet.put(PlayableProperty.CREATOR_URN, creatorId == Consts.NOT_SET ? Urn.NOT_SET : Urn.forUser(creatorId));
    }

    private void addOptionalOfflineSyncDates(CursorReader cursorReader, PropertySet propertySet) {
        final Date defaultDate = new Date(0);
        final Date removedAt = getDateOr(cursorReader, SoundView.OFFLINE_REMOVED_AT, defaultDate);
        final Date downloadedAt = getDateOr(cursorReader, SoundView.OFFLINE_DOWNLOADED_AT, defaultDate);

        if (isMostRecentDate(downloadedAt, removedAt)) {
            propertySet.put(OfflineProperty.DOWNLOAD_STATE, DownloadState.DOWNLOADED);
        } else if (isMostRecentDate(removedAt, downloadedAt)) {
            propertySet.put(OfflineProperty.DOWNLOAD_STATE, DownloadState.NO_OFFLINE);
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
