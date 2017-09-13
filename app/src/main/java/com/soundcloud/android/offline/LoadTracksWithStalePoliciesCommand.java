package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.IsOfflineLikedTracksEnabledCommand.isOfflineLikesEnabledQuery;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.likes.LikesStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.tracks.TrackPolicyStorage;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.PropellerDatabase;
import io.reactivex.Observable;
import io.reactivex.Single;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

class LoadTracksWithStalePoliciesCommand extends Command<Void, Collection<Urn>> {

    private final PropellerDatabase database;
    private final LikesStorage likesStorage;
    private final TrackPolicyStorage trackPolicyStorage;
    private final LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand;
    private final LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand;

    @Inject
    public LoadTracksWithStalePoliciesCommand(PropellerDatabase database,
                                              LikesStorage likesStorage,
                                              TrackPolicyStorage trackPolicyStorage,
                                              LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand,
                                              LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand) {
        this.database = database;
        this.likesStorage = likesStorage;
        this.trackPolicyStorage = trackPolicyStorage;
        this.loadOfflinePlaylistsCommand = loadOfflinePlaylistsCommand;
        this.loadPlaylistTrackUrnsCommand = loadPlaylistTrackUrnsCommand;
    }

    @Override
    public Collection<Urn> call(Void ignored) {
        final Collection<Urn> set = new TreeSet<>();
        if (isOfflineLikesEnabled()) {
            set.addAll(staleTracksFromlikes());
        }
        set.addAll(staleTracksFromPlaylists());
        return set;
    }

    private Set<Urn> staleTracksFromlikes() {
        return likesStorage.loadTrackLikes()
                           .map(associations -> Lists.transform(associations, UrnHolder::urn))
                           .flatMap(this::getStaleTracks)
                           .blockingGet();
    }

    @NonNull
    private Single<Set<Urn>> getStaleTracks(List<Urn> source) {
        return trackPolicyStorage.filterForStalePolicies(new HashSet<>(source), new Date(staleTime()));
    }

    private Set<Urn> staleTracksFromPlaylists() {
        return Observable.fromIterable(loadOfflinePlaylistsCommand.call())
                         .flatMapSingle(loadPlaylistTrackUrnsCommand::toSingle)
                         .flatMapSingle(this::getStaleTracks)
                         .collect((Callable<Set<Urn>>) HashSet::new, Set::addAll)
                         .blockingGet();
    }

    private long staleTime() {
        return System.currentTimeMillis() - PolicyOperations.POLICY_STALE_AGE_MILLISECONDS;
    }

    private boolean isOfflineLikesEnabled() {
        return database.query(isOfflineLikesEnabledQuery()).firstOrDefault(Boolean.class, false);
    }
}
