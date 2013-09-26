package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.tracking.eventlogger.TrackingInfo;

import android.net.Uri;
import android.text.TextUtils;

public class PlayQueueUri {
    // these will be stored as uri parameters
    private static final String PARAM_PLAYLIST_POS = "playlistPos";
    private static final String PARAM_SEEK_POS = "seekPos";
    private static final String PARAM_TRACK_ID = "trackId";

    private static final String PARAM_TRACKING = "tracking";

    public final Uri uri;

    public PlayQueueUri() {
        this(Content.PLAY_QUEUE.uri);
    }

    public PlayQueueUri(String uri) {
        this(Uri.parse(uri));
    }

    public PlayQueueUri(Uri uri) {
        this.uri = uri == null ? Content.PLAY_QUEUE.uri : uri;
    }

    public boolean isCollectionUri() {
        return Content.match(uri).isCollectionItem();
    }

    public int getSeekPos() {
        return extractValue(PARAM_SEEK_POS, 0);
    }

    public int getPos() {
        return extractValue(PARAM_PLAYLIST_POS, 0);
    }

    public int getTrackId() {
        return extractValue(PARAM_TRACK_ID, 0);
    }

    public TrackingInfo getTrackingInfo() {
        return TrackingInfo.fromUriParams(uri);
    }

    public Uri toUri(Track track, int mPlayPos, long seekPos, TrackingInfo trackingInfo) {
        return toUri(track == null ? -1l : track.getId(), mPlayPos, seekPos, trackingInfo);
    }

    public Uri toUri(long trackId, int mPlayPos, long seekPos, TrackingInfo trackingInfo) {
        Uri.Builder builder = uri.buildUpon().query(null); //clear the query for the new params
        if (trackId != -1l) {
            builder.appendQueryParameter(PARAM_TRACK_ID, String.valueOf(trackId));
        }
        builder.appendQueryParameter(PARAM_PLAYLIST_POS, String.valueOf(mPlayPos));
        builder.appendQueryParameter(PARAM_SEEK_POS, String.valueOf(seekPos));
        if (trackingInfo != null){
            trackingInfo.appendAsQueryParams(builder);
        }
        return builder.build();
    }


    private int extractValue(String parameter, final int defaultValue) {
        final String pos = uri.getQueryParameter(parameter);
        if (!TextUtils.isEmpty(pos)) {
            try {
                return Integer.parseInt(pos);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }
}
