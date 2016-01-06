package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Banana;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import static com.soundcloud.java.collections.Lists.transform;

public class StoreProfileCommand extends Command<UserProfileRecord, Boolean> {
    private static final Function<TrackRecordHolder, TrackRecord> TRACK_RECORD_HOLDER_TO_TRACK_RECORD = new Function<TrackRecordHolder, TrackRecord>() {
        public TrackRecord apply(TrackRecordHolder apiTrack) {
            return apiTrack.getTrackRecord();
        }
    };

    private static final Function<PlaylistRecordHolder, PlaylistRecord> PLAYLIST_RECORD_HOLDER_TO_PLAYLIST_RECORD = new Function<PlaylistRecordHolder, PlaylistRecord>() {
        public PlaylistRecord apply(PlaylistRecordHolder playlist) {
            return playlist.getPlaylistRecord();
        }
    };

    private final StoreTracksCommand storeTracksCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreUsersCommand storeUsersCommand;

    @Inject
    protected StoreProfileCommand(StoreTracksCommand storeTracksCommand,
                                  StorePlaylistsCommand storePlaylistsCommand,
                                  StoreUsersCommand storeUsersCommand) {
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
    }

    @Override
    public Boolean call(UserProfileRecord profile) {
        final Pair<List<TrackRecord>, List<PlaylistRecord>> playablesFromSpotlight = GET_PLAYABLES_FROM_PROPERTY_SET_SOURCE_HOLDER(profile.getSpotlight());
        final List<TrackRecord> playablesFromTracks = transform(profile.getTracks().getCollection(), TRACK_RECORD_HOLDER_TO_TRACK_RECORD);
        final List<PlaylistRecord> playablesFromReleases = transform(profile.getReleases().getCollection(), PLAYLIST_RECORD_HOLDER_TO_PLAYLIST_RECORD);
        final List<PlaylistRecord> playablesFromPlaylists = transform(profile.getPlaylists().getCollection(), PLAYLIST_RECORD_HOLDER_TO_PLAYLIST_RECORD);
        final Pair<List<TrackRecord>, List<PlaylistRecord>> playablesFromReposts = GET_PLAYABLES_FROM_PROPERTY_SET_SOURCE_HOLDER(profile.getReposts());
        final Pair<List<TrackRecord>, List<PlaylistRecord>> playablesFromLikes = GET_PLAYABLES_FROM_PROPERTY_SET_SOURCE_HOLDER(profile.getLikes());

        Iterable<TrackRecord> tracks = Iterables.concat(
                playablesFromSpotlight.first(),
                playablesFromTracks,
                playablesFromReposts.first(),
                playablesFromLikes.first());

        Iterable<PlaylistRecord> playlists = Iterables.concat(
                playablesFromSpotlight.second(),
                playablesFromReleases,
                playablesFromPlaylists,
                playablesFromReposts.second(),
                playablesFromLikes.second());

        List<UserRecord> users = Collections.singletonList(profile.getUser());

        return ((Iterables.isEmpty(tracks) || storeTracksCommand.call(tracks).success()) &&
                (Iterables.isEmpty(playlists) || storePlaylistsCommand.call(playlists).success()) &&
                storeUsersCommand.call(users).success());
    }

    private static Pair<List<TrackRecord>, List<PlaylistRecord>> GET_PLAYABLES_FROM_PROPERTY_SET_SOURCE_HOLDER(ModelCollection<? extends BananaHolder> propertySetSourceHolders) {
        List<TrackRecord> tracks = new ArrayList<>();
        List<PlaylistRecord> playlists = new ArrayList<>();

        for (BananaHolder bananaHolder : propertySetSourceHolders) {
            final Optional<Banana> propertySetSource = bananaHolder.getItem();

            if (propertySetSource.isPresent()) {
                if (propertySetSource.get() instanceof TrackRecordHolder) {
                    tracks.add(((TrackRecordHolder) propertySetSource.get()).getTrackRecord());
                } else if (propertySetSource.get() instanceof PlaylistRecordHolder) {
                    playlists.add(((PlaylistRecordHolder) propertySetSource.get()).getPlaylistRecord());
                }
            }
        }

        return Pair.of(tracks, playlists);
    }
}
