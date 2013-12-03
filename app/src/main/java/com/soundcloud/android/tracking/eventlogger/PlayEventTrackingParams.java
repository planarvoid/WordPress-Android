package com.soundcloud.android.tracking.eventlogger;

import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.ClientUri;

import android.content.ContentValues;

import java.util.UUID;

class PlayEventTrackingParams {
    final PlaybackEventData mPlaybackEventData;
    final long mTimeStamp;

    PlayEventTrackingParams(PlaybackEventData playbackEventData) {
        mPlaybackEventData = playbackEventData;
        mTimeStamp = System.currentTimeMillis();
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(PlayEventTrackingDbHelper.TrackingEvents.TIMESTAMP, mTimeStamp);
        values.put(PlayEventTrackingDbHelper.TrackingEvents.ACTION, mPlaybackEventData.getAction().toApiName());
        values.put(PlayEventTrackingDbHelper.TrackingEvents.SOUND_URN, ClientUri.forTrack(mPlaybackEventData.getTrack().getId()).toString());
        values.put(PlayEventTrackingDbHelper.TrackingEvents.SOUND_DURATION, mPlaybackEventData.getTrack().duration);
        values.put(PlayEventTrackingDbHelper.TrackingEvents.USER_URN, buildUserUrn(mPlaybackEventData.getUserId()));
        values.put(PlayEventTrackingDbHelper.TrackingEvents.SOURCE_INFO, mPlaybackEventData.getEventLoggerParams());
        return values;

    }

    private String buildUserUrn(final long userId) {
        if (userId < 0) {
            return "anonymous:" + UUID.randomUUID();
        } else {
            return ClientUri.forUser(userId).toString();
        }
    }

    @Override
    public String toString() {
        return "TrackingParams{" +
                "playback_event_data=" + mPlaybackEventData +
                ", timestamp=" + mTimeStamp +
                '}';
    }
}
