package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.playlists.PlaylistTrackProperty;
import com.soundcloud.android.playlists.RemovePlaylistCommand;
import com.soundcloud.java.collections.PropertySet;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class SinglePlaylistSyncer implements Callable<Boolean> {

    private final LoadPlaylistTracksWithChangesCommand loadPlaylistTracks;
    private final ApiClient apiClient;
    private final FetchPlaylistWithTracksCommand fetchPlaylistWithTracks;
    private final StoreTracksCommand storeTracks;
    private final StorePlaylistsCommand storePlaylists;
    private final ReplacePlaylistTracksCommand replacePlaylistTracks;
    private final RemovePlaylistCommand removePlaylist;
    private final PlaylistStorage playlistStorage;

    SinglePlaylistSyncer(FetchPlaylistWithTracksCommand fetchPlaylistWithTracks,
                         RemovePlaylistCommand removePlaylist,
                         LoadPlaylistTracksWithChangesCommand loadPlaylistTracks,
                         ApiClient apiClient, StoreTracksCommand storeTracks,
                         StorePlaylistsCommand storePlaylists,
                         ReplacePlaylistTracksCommand replacePlaylistTracks,
                         PlaylistStorage playlistStorage) {

        this.loadPlaylistTracks = loadPlaylistTracks;
        this.apiClient = apiClient;
        this.fetchPlaylistWithTracks = fetchPlaylistWithTracks;
        this.storeTracks = storeTracks;
        this.storePlaylists = storePlaylists;
        this.removePlaylist = removePlaylist;
        this.replacePlaylistTracks = replacePlaylistTracks;
        this.playlistStorage = playlistStorage;
    }

    @Override
    public Boolean call() throws Exception {
        // get remote playlist
        ApiPlaylistWithTracks apiPlaylistWithTracks;
        try {
            apiPlaylistWithTracks = fetchPlaylistWithTracks.call();
        } catch (ApiRequestException exception) {
            handleRemotePlaylistException(fetchPlaylistWithTracks.getInput(), exception);
            return true;
        }

        final List<Urn> remoteTracks = createRemoteTracklist(apiPlaylistWithTracks);

        // populate local sets
        final List<Urn> fullLocalTracklist = new ArrayList<>();
        final List<Urn> localRemovals = new ArrayList<>();
        final Set<Urn> localAdditions = new HashSet<>();

        final Urn playlistUrn = apiPlaylistWithTracks.getPlaylist().getUrn();
        final PropertySet playlistModifications = playlistStorage.loadPlaylistModifications(playlistUrn);
        compileLocalPlaylistState(fullLocalTracklist, localRemovals, localAdditions);

        if (hasChangesToPush(playlistModifications, localRemovals, localAdditions)) {
            final boolean trustLocalState = !playlistModifications.isEmpty();
            final List<Urn> finalTracklist = compileFinalTrackList(trustLocalState, remoteTracks, fullLocalTracklist, localRemovals, localAdditions);
            final PlaylistRecord playlistRecord = pushPlaylistChangesToApi(finalTracklist, playlistUrn, playlistModifications);
            resolveLocalState(playlistRecord, apiPlaylistWithTracks, finalTracklist);
        } else {
            resolveLocalState(apiPlaylistWithTracks.getPlaylist(), apiPlaylistWithTracks, remoteTracks);
        }
        return true;
    }

    private List<Urn> compileFinalTrackList(boolean trustLocal, List<Urn> remoteTracks, List<Urn> localTracks, List<Urn> localRemovals, Set<Urn> localAdditions) {
        if (trustLocal) {
            return compileLocallyBasedFinalTrackList(remoteTracks, localTracks, localRemovals, localAdditions);
        } else {
            return compileRemoteBasedFinalTrackList(remoteTracks, localRemovals, localAdditions);
        }
    }

    @NonNull
    private List<Urn> compileLocallyBasedFinalTrackList(List<Urn> remoteTracks, List<Urn> localTracks, List<Urn> localRemovals, Set<Urn> localAdditions) {
        // add all local tracks that have not been removed remotely
        List<Urn> finalTrackList = new ArrayList<>(Math.max(remoteTracks.size(), localTracks.size()));
        for (Urn track : localTracks) {
            if (remoteTracks.contains(track) || localAdditions.contains(track)) {
                finalTrackList.add(track);
            }
        }

        // add new remote tracks to the end
        finalTrackList.addAll(getListDifference(remoteTracks, localTracks, localRemovals));
        return finalTrackList;
    }

    @NonNull
    private List<Urn> compileRemoteBasedFinalTrackList(List<Urn> remoteTracks, List<Urn> localRemovals, Set<Urn> localAdditions) {
        // add all local tracks that have not been removed remotely
        List<Urn> finalTrackList = new ArrayList<>(remoteTracks.size() + localAdditions.size());
        for (Urn track : remoteTracks) {
            if (!localRemovals.contains(track)) {
                finalTrackList.add(track);
            }
        }

        // add new local tracks to the end
        finalTrackList.addAll(localAdditions);
        return finalTrackList;
    }

    private boolean hasChangesToPush(PropertySet localPlaylist, List<Urn> localRemovals, Set<Urn> localAdditions) {
        return !localPlaylist.isEmpty() || !localRemovals.isEmpty() || !localAdditions.isEmpty();
    }

    private PublicApiPlaylist pushPlaylistChangesToApi(List<Urn> finalTrackList, Urn playlistUrn, PropertySet localPlaylist) throws ApiRequestException, IOException, ApiMapperException {
        final ApiRequest request =
                ApiRequest.put(ApiEndpoints.LEGACY_PLAYLIST_DETAILS.path(playlistUrn.getNumericId()))
                        .forPublicApi()
                        .withContent(Collections.singletonMap("playlist", PlaylistApiUpdateObject.create(localPlaylist, finalTrackList)))
                        .build();
        return apiClient.fetchMappedResponse(request, PublicApiPlaylist.class);
    }

    private void resolveLocalState(PlaylistRecord playlistRecord, ApiPlaylistWithTracks apiPlaylistWithTracks, List<Urn> finalTrackList) throws Exception {
        // store dependencies (all still valid tracks that came back)
        storeTracks.call(extractValidRemoteTracks(apiPlaylistWithTracks, finalTrackList));

        // store final playlist tracks
        replacePlaylistTracks.with(finalTrackList).call();

        // store final playlist metadata
        storePlaylists.call(Collections.singleton(playlistRecord));
    }

    private void handleRemotePlaylistException(Urn urn, ApiRequestException exception) throws Exception {
        switch (exception.reason()) {
            case NOT_FOUND:
            case NOT_ALLOWED:
                removePlaylist.call(urn);
                break;
            default:
                throw exception;
        }
    }

    private List<Urn> createRemoteTracklist(ApiPlaylistWithTracks playlist) {
        final ModelCollection<ApiTrack> playlistTracks = playlist.getPlaylistTracks();
        final List<Urn> remoteTracks = new ArrayList<>(playlistTracks.getCollection().size());

        for (ApiTrack apiTrack : playlistTracks.getCollection()){
            remoteTracks.add(apiTrack.getUrn());
        }
        return remoteTracks;
    }

    private void compileLocalPlaylistState(List<Urn> validLocalTracks, List<Urn> localRemovals, Set<Urn> localAdditions) throws Exception {
        for (PropertySet playlistTrack : loadPlaylistTracks.call()){
            if (playlistTrack.contains(PlaylistTrackProperty.REMOVED_AT)){
                localRemovals.add(playlistTrack.get(PlaylistTrackProperty.TRACK_URN));
            } else {
                validLocalTracks.add(playlistTrack.get(PlaylistTrackProperty.TRACK_URN));
                if (playlistTrack.contains(PlaylistTrackProperty.ADDED_AT)){
                    localAdditions.add(playlistTrack.get(PlaylistTrackProperty.TRACK_URN));
                }
            }
        }
    }

    private List<ApiTrack> extractValidRemoteTracks(ApiPlaylistWithTracks playlist, List<Urn> finalTrackList) throws Exception {
        List<ApiTrack> validRemoteTracks = new ArrayList<>();
        for (ApiTrack apiTrack : playlist.getPlaylistTracks().getCollection()){
            if (finalTrackList.contains(apiTrack.getUrn())){
                validRemoteTracks.add(apiTrack);
            }
        }
        return validRemoteTracks;
    }

    private List<Urn> getListDifference(List<Urn> list, List<Urn>... without) {
        final List<Urn> difference = new ArrayList<>();
        difference.addAll(list);
        for (Collection<Urn> toRemove : without) {
            difference.removeAll(toRemove);
        }
        return difference;
    }
}
