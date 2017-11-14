package com.soundcloud.android.profile;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.android.users.UserRecordHolder;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class WriteMixedRecordsCommand extends Command<Iterable<? extends RecordHolder>, Boolean> {

    private final TrackRepository trackRepository;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreUsersCommand storeUsersCommand;

    @Inject
    protected WriteMixedRecordsCommand(TrackRepository trackRepository,
                                       StorePlaylistsCommand storePlaylistsCommand,
                                       StoreUsersCommand storeUsersCommand) {
        this.trackRepository = trackRepository;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
    }

    @Override
    public Boolean call(Iterable<? extends RecordHolder> collection) {
        List<TrackRecord> tracks = new ArrayList<>();
        List<PlaylistRecord> playlists = new ArrayList<>();
        List<UserRecord> users = new ArrayList<>();

        for (RecordHolder entity : collection) {
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
        return ((tracks.isEmpty() || trackRepository.storeTracks(tracks)) &&
                (playlists.isEmpty() || storePlaylistsCommand.call(playlists).success()) &&
                (users.isEmpty() || storeUsersCommand.call(users).success()));
    }

}
