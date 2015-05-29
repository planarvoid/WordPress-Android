package com.soundcloud.android.view.adapters;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PlayableItem;

import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class MixedPlayableItemClickListener {

    private final PlaybackOperations playbackOperations;
    private final Provider<ExpandPlayerSubscriber> subscriberProvider;
    private final Screen screen;
    private final SearchQuerySourceInfo searchQuerySourceInfo;

    public MixedPlayableItemClickListener(PlaybackOperations playbackOperations, Provider<ExpandPlayerSubscriber> subscriberProvider, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        this.playbackOperations = playbackOperations;
        this.subscriberProvider = subscriberProvider;
        this.screen = screen;
        this.searchQuerySourceInfo = searchQuerySourceInfo;
    }

    public void onItemClick(List<PlayableItem> playables, View view, int position) {
        final ListItem item = playables.get(position);
        final Urn playableUrn = item.getEntityUrn();
        if (playableUrn.isTrack()) {
            final List<Urn> trackUrns = filterTracks(playables);
            final int adjustedPosition = filterTracks(playables.subList(0, position)).size();
            final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
            playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
            playbackOperations
                    .playTracks(trackUrns, adjustedPosition, playSessionSource)
                    .subscribe(subscriberProvider.get());
        } else if (playableUrn.isPlaylist()){
            PlaylistDetailActivity.start(view.getContext(), playableUrn, Screen.SIDE_MENU_STREAM);
        } else {
            throw new IllegalArgumentException("Unrecognized urn in MixedPlayableAdapter: " + playableUrn);
        }
    }

    private List<Urn> filterTracks(List<PlayableItem> data){
        ArrayList<Urn> urns = new ArrayList<>(data.size());
        for (ListItem listItem : data){
            if (listItem.getEntityUrn().isTrack()){
                urns.add(listItem.getEntityUrn());
            }
        }
        return urns;
    }

    public static class Factory {

        private final PlaybackOperations playbackOperations;
        private final Provider<ExpandPlayerSubscriber> subscriberProvider;

        @Inject
        public Factory(PlaybackOperations playbackOperations, Provider<ExpandPlayerSubscriber> subscriberProvider){
            this.playbackOperations = playbackOperations;
            this.subscriberProvider = subscriberProvider;
        }

        public MixedPlayableItemClickListener create(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo){
            return new MixedPlayableItemClickListener(playbackOperations, subscriberProvider, screen, searchQuerySourceInfo);
        }
    }
}
