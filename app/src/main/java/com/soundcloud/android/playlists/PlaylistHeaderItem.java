package com.soundcloud.android.playlists;

import com.soundcloud.android.playback.PlaySessionSource;

public class PlaylistHeaderItem extends PlaylistItem {

    private final PlaySessionSource playSessionSource;
    private final PlaylistWithTracks source;

    private PlaylistHeaderItem(PlaylistWithTracks source, PlaySessionSource playSessionSource) {
        super(source.getSourceSet());
        this.playSessionSource = playSessionSource;
        this.source = source;
    }

    public PlaySessionSource getPlaySessionSource() {
        return playSessionSource;
    }

    public static PlaylistHeaderItem create(PlaylistWithTracks playlist, PlaySessionSource playSessionSource) {
        return new PlaylistHeaderItem(playlist, playSessionSource);
    }

    public String geFormattedDuration() {
        return source.getDuration();
    }

    public boolean showPlayButton() {
        return !source.getTracks().isEmpty();
    }
}
