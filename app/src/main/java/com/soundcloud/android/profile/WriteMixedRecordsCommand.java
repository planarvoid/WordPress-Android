package com.soundcloud.android.profile;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.android.users.UserRecordHolder;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class WriteMixedRecordsCommand extends Command<Iterable<? extends PropertySetSource>, Boolean> {

    private final StoreTracksCommand storeTracksCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreUsersCommand storeUsersCommand;

    @Inject
    protected WriteMixedRecordsCommand(StoreTracksCommand storeTracksCommand,
                                       StorePlaylistsCommand storePlaylistsCommand, StoreUsersCommand storeUsersCommand) {
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
    }

    @Override
    public Boolean call(Iterable<? extends PropertySetSource> collection) {
        List<TrackRecord> tracks = new ArrayList<>();
        List<PlaylistRecord> playlists = new ArrayList<>();
        List<UserRecord> users = new ArrayList<>();

        for (PropertySetSource entity : collection) {
            if (entity instanceof TrackRecordHolder) {
                tracks.add(((TrackRecordHolder) entity).getTrackRecord());
            }
            if (entity instanceof PlaylistRecordHolder) {
                playlists.add(((PlaylistRecordHolder) entity).getPlaylistRecord());
            }
            if (entity instanceof UserRecordHolder) {
                users.add(((UserRecordHolder) entity).getUserRecord());
            }
        }
        return ((tracks.isEmpty() || storeTracksCommand.call(tracks).success()) &&
                (playlists.isEmpty() || storePlaylistsCommand.call(playlists).success()) &&
                (users.isEmpty() || storeUsersCommand.call(users).success()));
    }

}
