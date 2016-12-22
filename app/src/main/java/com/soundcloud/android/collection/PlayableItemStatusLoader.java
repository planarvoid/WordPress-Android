package com.soundcloud.android.collection;

import static com.soundcloud.android.utils.Urns.playlistPredicate;
import static com.soundcloud.android.utils.Urns.trackPredicate;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.MoreCollections.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;

import javax.inject.Inject;
import java.util.Map;

public class PlayableItemStatusLoader extends Command<Iterable<PropertySet>, Iterable<PropertySet>> {
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
    public Iterable<PropertySet> call(Iterable<PropertySet> input) {
        final Iterable<PropertySet> playlistPropertySets = filteredItems(input, playlistPredicate());
        final Map<Urn, PropertySet> playlistRepostStatus = loadPlaylistRepostStatuses.call(playlistPropertySets);
        final Map<Urn, PropertySet> playlistLikedStatus = loadPlaylistLikedStatuses.call(playlistPropertySets);
        updatePropertySets(playlistPropertySets, playlistRepostStatus, playlistLikedStatus);

        final Iterable<PropertySet> trackPropertySets = filteredItems(input, trackPredicate());
        final Map<Urn, PropertySet> trackRepostStatus = loadTrackRepostStatuses.call(trackPropertySets);
        final Map<Urn, PropertySet> trackLikedStatus = loadTrackLikedStatuses.call(trackPropertySets);
        updatePropertySets(trackPropertySets, trackRepostStatus, trackLikedStatus);

        return input;
    }

    private void updatePropertySets(Iterable<PropertySet> propertySets,
                                    Map<Urn, PropertySet> repostStatuses,
                                    Map<Urn, PropertySet> likedStatuses) {

        for (final PropertySet resultItem : propertySets) {
            final Urn itemUrn = resultItem.getOrElse(PlayableProperty.URN, Urn.NOT_SET);

            if (repostStatuses.containsKey(itemUrn)) {
                resultItem.update(repostStatuses.get(itemUrn));
            }

            if (likedStatuses.containsKey(itemUrn)) {
                resultItem.update(likedStatuses.get(itemUrn));
            }
        }
    }

    private Iterable<PropertySet> filteredItems(Iterable<PropertySet> propertySets, final Predicate<Urn> urnPredicate) {
        return filter(newArrayList(propertySets), input -> urnPredicate.apply(input.getOrElse(PlayableProperty.URN, Urn.NOT_SET)));
    }
}
