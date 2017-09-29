package com.soundcloud.android.offline;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.likes.LikesStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.tracks.TrackPolicyStorage;
import com.soundcloud.java.collections.Lists;
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

    private final LikesStorage likesStorage;
    private final TrackPolicyStorage trackPolicyStorage;
    private final LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand;
    private final OfflineContentStorage offlineContentStorage;

    @Inject
    public LoadTracksWithStalePoliciesCommand(LikesStorage likesStorage,
                                              TrackPolicyStorage trackPolicyStorage,
                                              LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand,
                                              OfflineContentStorage offlineContentStorage) {
        this.likesStorage = likesStorage;
        this.trackPolicyStorage = trackPolicyStorage;
        this.loadPlaylistTrackUrnsCommand = loadPlaylistTrackUrnsCommand;
        this.offlineContentStorage = offlineContentStorage;
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
        return offlineContentStorage.getOfflinePlaylists()
                                    .flatMapObservable(Observable::fromIterable)
                                    .flatMapSingle(loadPlaylistTrackUrnsCommand::toSingle)
                                    .flatMapSingle(this::getStaleTracks)
                                    .collect((Callable<Set<Urn>>) HashSet::new, Set::addAll)
                                    .blockingGet();
    }

    private long staleTime() {
        return System.currentTimeMillis() - PolicyOperations.POLICY_STALE_AGE_MILLISECONDS;
    }

    private boolean isOfflineLikesEnabled() {
        return offlineContentStorage.isOfflineLikesEnabled().blockingGet();
    }
}
