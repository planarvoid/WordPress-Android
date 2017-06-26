package com.soundcloud.android.discovery.systemplaylist;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.newforyou.NewForYou;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;

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

    static SystemPlaylist map(Resources resources, NewForYou newForYou) {
        return SystemPlaylist.create(
                Urn.NOT_SET,
                Optional.of(newForYou.queryUrn()),
                Optional.of(resources.getString(R.string.new_for_you_title)),
                Optional.of(resources.getString(R.string.new_for_you_intro)),
                newForYou.tracks(),
                Optional.of(newForYou.lastUpdate()),
                Optional.absent(),
                Optional.absent()
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
