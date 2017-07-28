package com.soundcloud.android.testsupport;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.EntityItemEmitter;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Maps;
import io.reactivex.Observable;

import java.util.List;
import java.util.Map;

public class TestEntityItemEmitter implements EntityItemEmitter {


    public TestEntityItemEmitter() {
    }

    @Override
    public Observable<List<TrackItem>> trackItems(List<ApiTrack> apiTracks) {
        return Observable.just(ModelFixtures.trackItems(apiTracks));
    }

    @Override
    public Observable<Map<Urn, TrackItem>> trackItemsAsMap(List<ApiTrack> apiTracks) {
        return trackItems(apiTracks).map(trackItems -> Maps.asMap(trackItems, TrackItem::getUrn));
    }

    @Override
    public Observable<List<PlaylistItem>> playlistItems(List<ApiPlaylist> apiPlaylists) {
        return Observable.just(ModelFixtures.playlistItems(apiPlaylists));
    }

    @Override
    public Observable<Map<Urn, PlaylistItem>> playlistItemsAsMap(List<ApiPlaylist> apiPlaylists) {
        return playlistItems(apiPlaylists).map(playlistItems -> Maps.asMap(playlistItems, PlaylistItem::getUrn));
    }

    @Override
    public Observable<List<UserItem>> userItems(List<ApiUser> apiUsers) {
        return Observable.just(ModelFixtures.userItems(apiUsers));
    }

    @Override
    public Observable<Map<Urn, UserItem>> userItemsAsMap(List<ApiUser> apiUsers) {
        return userItems(apiUsers).map(userItems -> Maps.asMap(userItems, UserItem::getUrn));
    }
}
