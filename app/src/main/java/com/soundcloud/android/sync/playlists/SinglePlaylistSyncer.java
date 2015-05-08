package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistTrackProperty;
import com.soundcloud.propeller.PropertySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

class SinglePlaylistSyncer implements Callable<Boolean> {

    private final LoadPlaylistTracksWithChangesCommand loadPlaylistTracks;
    private final PushPlaylistAdditionsCommand pushPlaylistAdditions;
    private final PushPlaylistRemovalsCommand pushPlaylistRemovals;
    private final FetchPlaylistWithTracksCommand fetchPlaylistWithTracks;
    private final StoreTracksCommand storeTracks;
    private final StorePlaylistCommand storePlaylist;
    private final ReplacePlaylistTracksCommand replacePlaylistTracks;
    private final RemovePlaylistCommand removePlaylist;

    SinglePlaylistSyncer(FetchPlaylistWithTracksCommand fetchPlaylistWithTracks,
                         RemovePlaylistCommand removePlaylist,
                         LoadPlaylistTracksWithChangesCommand loadPlaylistTracks,
                         PushPlaylistAdditionsCommand pushPlaylistAdditions,
                         PushPlaylistRemovalsCommand pushPlaylistRemovals,
                         StoreTracksCommand storeTracks,
                         StorePlaylistCommand storePlaylist,
                         ReplacePlaylistTracksCommand replacePlaylistTracks) {

        this.loadPlaylistTracks = loadPlaylistTracks;
        this.pushPlaylistAdditions = pushPlaylistAdditions;
        this.pushPlaylistRemovals = pushPlaylistRemovals;
        this.fetchPlaylistWithTracks = fetchPlaylistWithTracks;
        this.storeTracks = storeTracks;
        this.storePlaylist = storePlaylist;
        this.removePlaylist = removePlaylist;
        this.replacePlaylistTracks = replacePlaylistTracks;
    }

    @Override
    public Boolean call() throws Exception {

        // get remote playlist
        ApiPlaylistWithTracks apiPlaylistWithTracks;
        try {
            apiPlaylistWithTracks = fetchPlaylistWithTracks.call();
        } catch (ApiRequestException exception) {
            handleRemotePlaylistException(exception);
            return true;
        }

        final Set<Urn> remoteTracks = createRemoteTracklist(apiPlaylistWithTracks);

        //populate local sets
        final Set<Urn> localCleanTracks = new TreeSet<>();
        final Set<Urn> localAdditions = new TreeSet<>();
        final Set<Urn> localRemovals = new TreeSet<>();
        compileLocalPlaylistState(localCleanTracks, localAdditions, localRemovals);

        // perform remote removals
        final Set<Urn> pendingRemoteRemovals = getSetIntersection(localRemovals, remoteTracks);
        List<ApiTrack> validRemoteTracks = performRemovals(apiPlaylistWithTracks, pendingRemoteRemovals);

        // store dependencies (all still valid tracks that came back)
        storeTracks.call(validRemoteTracks);

        // perform remote additions
        final Set<Urn> pendingRemoteAdditions = getSetDifference(localAdditions, remoteTracks);
        List<Urn> finalTrackList = performAdditions(validRemoteTracks, pendingRemoteAdditions);

        // store final playlist tracks
        replacePlaylistTracks.with(finalTrackList).call();

        updateLocalPlaylist(apiPlaylistWithTracks.getPlaylist(), pendingRemoteRemovals, pendingRemoteAdditions);
        return true;
    }

    private void handleRemotePlaylistException(ApiRequestException exception) throws Exception {
        switch (exception.reason()) {
            case NOT_FOUND:
            case NOT_ALLOWED:
                removePlaylist.call();
                break;
            default:
                throw exception;
        }
    }

    private Set<Urn> createRemoteTracklist(ApiPlaylistWithTracks playlist) {
        final Set<Urn> remoteTracks = new TreeSet<>();
        final ModelCollection<ApiTrack> playlistTracks = playlist.getPlaylistTracks();

        for (ApiTrack apiTrack : playlistTracks.getCollection()){
            remoteTracks.add(apiTrack.getUrn());
        }
        return remoteTracks;
    }

    private void compileLocalPlaylistState(Set<Urn> localCleanTracks, Set<Urn> localAdditions, Set<Urn> localRemovals) throws Exception {
        for (PropertySet playlistTrack : loadPlaylistTracks.call()){
            if (playlistTrack.contains(PlaylistTrackProperty.REMOVED_AT)){
                localRemovals.add(playlistTrack.get(PlaylistTrackProperty.TRACK_URN));
            } else if (playlistTrack.contains(PlaylistTrackProperty.ADDED_AT)){
                localAdditions.add(playlistTrack.get(PlaylistTrackProperty.TRACK_URN));
            } else {
                localCleanTracks.add(playlistTrack.get(PlaylistTrackProperty.TRACK_URN));
            }
        }
    }

    private List<ApiTrack> performRemovals(ApiPlaylistWithTracks playlist, Set<Urn> pendingRemoteRemovals) throws Exception {
        // remove remote tracks
        pushPlaylistRemovals.with(pendingRemoteRemovals).call();

        // filter out removed tracks
        List<ApiTrack> validRemoteTracks = new ArrayList<>();
        for (ApiTrack apiTrack : playlist.getPlaylistTracks().getCollection()){
            if (!pendingRemoteRemovals.contains(apiTrack.getUrn())){
                validRemoteTracks.add(apiTrack);
            }
        }
        return validRemoteTracks;
    }

    private List<Urn> performAdditions(List<ApiTrack> validRemoteTracks, Set<Urn> pendingRemoteAdditions) throws Exception {
        Collection<Urn> additions = pushPlaylistAdditions.with(pendingRemoteAdditions).call(); // could indiv. tracks fail?

        // compile api tracks + new additions
        List<Urn> newTrackList = new ArrayList<>(validRemoteTracks.size() + additions.size());
        for (ApiTrack track : validRemoteTracks){
            newTrackList.add(track.getUrn());
        }
        newTrackList.addAll(additions);
        return newTrackList;
    }

    private void updateLocalPlaylist(ApiPlaylist playlist, Set<Urn> pendingRemoteRemovals, Set<Urn> pendingRemoteAdditions) throws com.soundcloud.propeller.PropellerWriteException {

        // update final track count with new additions + removals
        final int originalTrackCount = playlist.getTrackCount();
        playlist.setTrackCount(originalTrackCount + pendingRemoteAdditions.size() - pendingRemoteRemovals.size());

        // store final playlist metadata
        storePlaylist.with(playlist).call();
    }

    @SafeVarargs
    private final Set<Urn> getSetDifference(Set<Urn> set, Set<Urn>... without) {
        final Set<Urn> difference = new TreeSet<>();
        difference.addAll(set);
        for (Set<Urn> s : without) {
            difference.removeAll(s);
        }
        return difference;
    }

    private Set<Urn> getSetIntersection(Set<Urn> set, Set<Urn> toIntersectWith) {
        final Set<Urn> intersection = new TreeSet<>();
        intersection.addAll(set);
        intersection.retainAll(toIntersectWith);
        return intersection;
    }
}