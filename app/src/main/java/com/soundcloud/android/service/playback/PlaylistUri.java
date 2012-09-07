package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;

import android.net.Uri;
import android.text.TextUtils;

public class PlaylistUri {
    // these will be stored as uri parameters
    private static final String PARAM_PLAYLIST_POS = "playlistPos";
    private static final String PARAM_SEEK_POS = "seekPos";
    private static final String PARAM_TRACK_ID = "trackId";

    private static final int DEFAULT_PLAYLIST = 0;
    public static final Uri DEFAULT = Content.PLAYLIST.forId(DEFAULT_PLAYLIST);

    public final Uri uri;

    public PlaylistUri() {
        this(DEFAULT);
    }

    public PlaylistUri(String uri) {
        this(Uri.parse(uri));
    }

    public PlaylistUri(Uri uri) {
        this.uri = uri == null ? DEFAULT : uri;
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

    public Uri toUri(Track track, int mPlayPos, long seekPos) {
        Uri.Builder builder = uri.buildUpon();
        if (track != null) {
            builder.appendQueryParameter(PARAM_TRACK_ID, String.valueOf(track.id));
        }
        builder.appendQueryParameter(PARAM_PLAYLIST_POS, String.valueOf(mPlayPos));
        builder.appendQueryParameter(PARAM_SEEK_POS, String.valueOf(seekPos));
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

    public boolean isDefault() {
        return uri.buildUpon().query(null).build().equals(DEFAULT);
    }
}
