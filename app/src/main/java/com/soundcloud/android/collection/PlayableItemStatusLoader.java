package com.soundcloud.android.collection;

import static com.soundcloud.android.utils.Urns.playlistPredicate;
import static com.soundcloud.android.utils.Urns.trackPredicate;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.MoreCollections.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;

import javax.inject.Inject;
import java.util.Map;

public class PlayableItemStatusLoader extends Command<Iterable<PlayableItem>, Iterable<PlayableItem>> {
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    private final LoadPlaylistRepostStatuses loadPlaylistRepostStatuses;
    private final LoadTrackLikedStatuses loadTrackLikedStatuses;
    private final LoadTrackRepostStatuses loadTrackRepostStatuses;

    @Inject
    public PlayableItemStatusLoader(LoadPlaylistLikedStatuses loadPlaylistLikedStatuses,
                                    LoadPlaylistRepostStatuses loadPlaylistRepostStatuses,
                                    LoadTrackLikedStatuses loadTrackLikedStatuses,
                                    LoadTrackRepostStatuses loadTrackRepostStatuses) {
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.loadPlaylistRepostStatuses = loadPlaylistRepostStatuses;
        this.loadTrackLikedStatuses = loadTrackLikedStatuses;
        this.loadTrackRepostStatuses = loadTrackRepostStatuses;
    }

    @Override
    public Iterable<PlayableItem> call(Iterable<PlayableItem> input) {
        final Iterable<PlayableItem> playlists = filteredItems(input, playlistPredicate());
        final Iterable<Urn> playlistUrns = Iterables.transform(playlists, PlayableItem::getUrn);
        final Map<Urn, Boolean> playlistRepostStatus = loadPlaylistRepostStatuses.call(playlistUrns);
        final Map<Urn, Boolean> playlistLikedStatus = loadPlaylistLikedStatuses.call(playlistUrns);
        updatePropertySets(input, playlistRepostStatus, playlistLikedStatus);

        final Iterable<PlayableItem> tracks = filteredItems(input, trackPredicate());
        final Iterable<Urn> trackUrns = Iterables.transform(tracks, PlayableItem::getUrn);
        final Map<Urn, Boolean> trackRepostStatus = loadTrackRepostStatuses.call(trackUrns);
        final Map<Urn, Boolean> trackLikedStatus = loadTrackLikedStatuses.call(trackUrns);
        updatePropertySets(input, trackRepostStatus, trackLikedStatus);

        return input;
    }

    private void updatePropertySets(Iterable<PlayableItem> playableItems,
                                    Map<Urn, Boolean> repostStatuses,
                                    Map<Urn, Boolean> likedStatuses) {

        for (final PlayableItem playableItem : playableItems) {
            final Urn itemUrn = playableItem.getUrn();

            if (repostStatuses.containsKey(itemUrn)) {
                playableItem.setRepostedByCurrentUser(repostStatuses.get(itemUrn));
            }

            if (likedStatuses.containsKey(itemUrn)) {
                playableItem.setLikedByCurrentUser(likedStatuses.get(itemUrn));
            }
        }
    }

    private Iterable<PlayableItem> filteredItems(Iterable<PlayableItem> propertySets, final Predicate<Urn> urnPredicate) {
        return filter(newArrayList(propertySets), input -> urnPredicate.apply(input.getUrn()));
    }
}
