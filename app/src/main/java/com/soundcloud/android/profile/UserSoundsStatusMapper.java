package com.soundcloud.android.profile;

import static com.soundcloud.java.collections.MoreCollections.filter;
import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.collection.LoadPlaylistRepostStatuses;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.collection.LoadTrackLikedStatuses;
import com.soundcloud.android.collection.LoadTrackRepostStatuses;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import org.jetbrains.annotations.Nullable;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class UserSoundsStatusMapper {
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    private final LoadPlaylistRepostStatuses loadPlaylistRepostStatuses;
    private final LoadTrackLikedStatuses loadTrackLikedStatuses;
    private final LoadTrackRepostStatuses loadTrackRepostStatuses;

    @Inject
    public UserSoundsStatusMapper(LoadPlaylistLikedStatuses loadPlaylistLikedStatuses,
                                  LoadPlaylistRepostStatuses loadPlaylistRepostStatuses,
                                  LoadTrackLikedStatuses loadTrackLikedStatuses,
                                  LoadTrackRepostStatuses loadTrackRepostStatuses) {
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.loadPlaylistRepostStatuses = loadPlaylistRepostStatuses;
        this.loadTrackLikedStatuses = loadTrackLikedStatuses;
        this.loadTrackRepostStatuses = loadTrackRepostStatuses;
    }

    public void map(List<UserSoundsItem> items) {
        final Collection<PropertySet> playlistPropertySets = transform(playlistItems(items), toPropertySets());
        final Map<Urn, PropertySet> playlistRepostStatus = loadPlaylistRepostStatuses.call(playlistPropertySets);
        final Map<Urn, PropertySet> playlistLikedStatus = loadPlaylistLikedStatuses.call(playlistPropertySets);

        updatePropertySets(playlistPropertySets, playlistRepostStatus, playlistLikedStatus);

        final Collection<PropertySet> trackPropertySets = transform(trackItems(items), toPropertySets());
        final Map<Urn, PropertySet> trackRepostStatus = loadTrackRepostStatuses.call(trackPropertySets);
        final Map<Urn, PropertySet> trackLikedStatus = loadTrackLikedStatuses.call(trackPropertySets);

        updatePropertySets(trackPropertySets, trackRepostStatus, trackLikedStatus);
    }

    private void updatePropertySets(Collection<PropertySet> propertySets,
                                    Map<Urn, PropertySet> repostStatuses,
                                    Map<Urn, PropertySet> likedStatuses) {
        for (final PropertySet resultItem : propertySets) {
            final Urn itemUrn = resultItem.getOrElse(UserProperty.URN, Urn.NOT_SET);

            if (repostStatuses.containsKey(itemUrn)) {
                resultItem.update(repostStatuses.get(itemUrn));
            }

            if (likedStatuses.containsKey(itemUrn)) {
                resultItem.update(likedStatuses.get(itemUrn));
            }
        }
    }

    private Collection<UserSoundsItem> playlistItems(List<UserSoundsItem> items) {
        return filter(items, new Predicate<UserSoundsItem>() {
            @Override
            public boolean apply(UserSoundsItem input) {
                return input.isPlaylist();
            }
        });
    }

    private Collection<UserSoundsItem> trackItems(List<UserSoundsItem> items) {
        return filter(items, new Predicate<UserSoundsItem>() {
            @Override
            public boolean apply(UserSoundsItem input) {
                return input.isTrack();
            }
        });
    }

    @NonNull
    private Function<UserSoundsItem, PropertySet> toPropertySets() {
        return new Function<UserSoundsItem, PropertySet>() {
            @Nullable
            @Override
            public PropertySet apply(UserSoundsItem input) {
                if (input.isPlaylist()) {
                    return input.getPlaylistItem().get().getSource();
                }
                return input.getTrackItem().get().getSource();
            }
        };
    }
}
