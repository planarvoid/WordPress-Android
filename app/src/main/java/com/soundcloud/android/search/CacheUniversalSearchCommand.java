package com.soundcloud.android.search;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.propeller.PropellerWriteException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class CacheUniversalSearchCommand extends LegacyCommand<Iterable<ApiUniversalSearchItem>, Void, CacheUniversalSearchCommand> {

    private final StoreTracksCommand storeTracksCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreUsersCommand storeUsersCommand;

    @Inject
    public CacheUniversalSearchCommand(StoreTracksCommand storeTracksCommand, StorePlaylistsCommand storePlaylistsCommand,
                                       StoreUsersCommand storeUsersCommand) {
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
    }

    @Override
    public Void call() throws PropellerWriteException {
        List<ApiUser> users = new ArrayList<>();
        List<ApiPlaylist> playlists = new ArrayList<>();
        List<ApiTrack> tracks = new ArrayList<>();

        for (ApiUniversalSearchItem result : input) {
            if (result.isUser()) {
                users.add(result.getUser());
            } else if (result.isPlaylist()) {
                playlists.add(result.getPlaylist());
            } else if (result.isTrack()) {
                tracks.add(result.getTrack());
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

        return null;
    }
}
