package com.soundcloud.android.discovery.systemplaylist;

import com.soundcloud.android.tracks.Track;
import com.soundcloud.java.collections.Lists;

import java.util.List;

final class SystemPlaylistMapper {

    static SystemPlaylist map(ApiSystemPlaylist apiSystemPlaylist) {
        return SystemPlaylist.create(
                apiSystemPlaylist.urn(),
                apiSystemPlaylist.tracks().getQueryUrn(),
                apiSystemPlaylist.title(),
                apiSystemPlaylist.description(),
                Lists.transform(apiSystemPlaylist.tracks().getCollection(), Track::from),
                apiSystemPlaylist.lastUpdated(),
                apiSystemPlaylist.artworkUrlTemplate(),
                apiSystemPlaylist.trackingFeatureName()
        );
    }

    static SystemPlaylist map(SystemPlaylistEntity systemPlaylistEntity, List<Track> tracks) {
        return SystemPlaylist.create(
                systemPlaylistEntity.urn(),
                systemPlaylistEntity.queryUrn(),
                systemPlaylistEntity.title(),
                systemPlaylistEntity.description(),
                tracks,
                systemPlaylistEntity.lastUpdated(),
                systemPlaylistEntity.artworkUrlTemplate(),
                systemPlaylistEntity.trackingFeatureName()
        );
    }

    private SystemPlaylistMapper() {}
}
