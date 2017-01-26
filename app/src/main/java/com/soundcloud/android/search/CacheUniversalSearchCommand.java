package com.soundcloud.android.search;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.PropellerWriteException;
import rx.functions.Action1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class CacheUniversalSearchCommand implements Action1<Iterable<ApiUniversalSearchItem>> {

    private final StoreTracksCommand storeTracksCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreUsersCommand storeUsersCommand;

    @Inject
    public CacheUniversalSearchCommand(StoreTracksCommand storeTracksCommand,
                                       StorePlaylistsCommand storePlaylistsCommand,
                                       StoreUsersCommand storeUsersCommand) {
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
    }

    @Override
    public void call(Iterable<ApiUniversalSearchItem> input) throws PropellerWriteException {
        List<ApiUser> users = new ArrayList<>();
        List<ApiPlaylist> playlists = new ArrayList<>();
        List<ApiTrack> tracks = new ArrayList<>();

        for (ApiUniversalSearchItem result : input) {
            final Optional<ApiUser> user = result.user();
            final Optional<ApiPlaylist> playlist = result.playlist();
            final Optional<ApiTrack> track = result.track();
            if (user.isPresent()) {
                users.add(user.get());
            } else if (playlist.isPresent()) {
                playlists.add(playlist.get());
            } else if (track.isPresent()) {
                tracks.add(track.get());
            }
        }

        if (!users.isEmpty()) {
            storeUsersCommand.call(users);
        }
        if (!playlists.isEmpty()) {
            storePlaylistsCommand.call(playlists);
        }
        if (!tracks.isEmpty()) {
            storeTracksCommand.call(tracks);
        }
    }
}
