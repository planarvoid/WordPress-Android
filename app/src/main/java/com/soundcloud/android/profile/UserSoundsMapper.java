package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserSoundsMapper implements Func1<UserProfileRecord, Iterable<UserSoundsItem>> {

    @Inject
    public UserSoundsMapper() {
    }

    @Override
    public Iterable<UserSoundsItem> call(UserProfileRecord userProfile) {
        final List<UserSoundsItem> items = new ArrayList<>();

        final ModelCollection<? extends ApiEntityHolderSource> spotlight = userProfile.getSpotlight();
        final ModelCollection<? extends ApiEntityHolder> tracks = userProfile.getTracks();
        final ModelCollection<? extends ApiEntityHolder> releases = userProfile.getReleases();
        final ModelCollection<? extends ApiEntityHolder> playlists = userProfile.getPlaylists();
        final ModelCollection<? extends ApiEntityHolderSource> reposts = userProfile.getReposts();
        final ModelCollection<? extends ApiEntityHolderSource> likes = userProfile.getLikes();

        if (!spotlight.getCollection().isEmpty()) {
            items.addAll(holderSourceCollectionToUserSoundItems(spotlight, UserSoundsTypes.SPOTLIGHT));
        }

        if (!tracks.getCollection().isEmpty()) {
            items.add(UserSoundsItem.fromHeader(UserSoundsTypes.TRACKS));

            items.addAll(Lists.transform(
                    tracks.getCollection(),
                    holderToUserSoundItems(UserSoundsTypes.TRACKS)));

            if (tracks.getNextLink().isPresent()) {
                items.add(UserSoundsItem.fromViewAll(UserSoundsTypes.TRACKS));
            }

            items.add(UserSoundsItem.fromDivider());
        }

        if (!releases.getCollection().isEmpty()) {
            items.add(UserSoundsItem.fromHeader(UserSoundsTypes.RELEASES));

            items.addAll(Lists.transform(
                    releases.getCollection(),
                    holderToUserSoundItems(UserSoundsTypes.RELEASES)));

            if (releases.getNextLink().isPresent()) {
                items.add(UserSoundsItem.fromViewAll(UserSoundsTypes.RELEASES));
            }

            items.add(UserSoundsItem.fromDivider());
        }

        if (!playlists.getCollection().isEmpty()) {
            items.add(UserSoundsItem.fromHeader(UserSoundsTypes.PLAYLISTS));

            items.addAll(Lists.transform(
                    playlists.getCollection(),
                    holderToUserSoundItems(UserSoundsTypes.PLAYLISTS)));

            if (playlists.getNextLink().isPresent()) {
                items.add(UserSoundsItem.fromViewAll(UserSoundsTypes.PLAYLISTS));
            }

            items.add(UserSoundsItem.fromDivider());
        }

        if (!reposts.getCollection().isEmpty()) {
            items.addAll(holderSourceCollectionToUserSoundItems(reposts, UserSoundsTypes.REPOSTS));
        }

        if (!likes.getCollection().isEmpty()) {
            items.addAll(holderSourceCollectionToUserSoundItems(likes, UserSoundsTypes.LIKES));
        }

        return items;
    }

    private Function<ApiEntityHolder, UserSoundsItem> holderToUserSoundItems(final int collectionType) {
        return new Function<ApiEntityHolder, UserSoundsItem>() {
            @Override
            public UserSoundsItem apply(ApiEntityHolder holder) {
                final PropertySet properties = holder.toPropertySet();

                if (properties.get(EntityProperty.URN).isTrack()) {
                    return UserSoundsItem.fromTrackItem(TrackItem.from(properties), collectionType);
                } else {
                    return UserSoundsItem.fromPlaylistItem(PlaylistItem.from(properties), collectionType);
                }
            }
        };
    }

    private Collection<UserSoundsItem> holderSourceCollectionToUserSoundItems(
            ModelCollection<? extends ApiEntityHolderSource> modelCollection,
            int collectionType) {
        List<UserSoundsItem> items = new ArrayList<>();

        items.add(UserSoundsItem.fromHeader(collectionType));

        items.addAll(Lists.transform(
                modelCollection.getCollection(),
                holderSourceToUserSoundItems(collectionType)));

        if (modelCollection.getNextLink().isPresent()) {
            items.add(UserSoundsItem.fromViewAll(collectionType));
        }

        items.add(UserSoundsItem.fromDivider());

        return items;
    }

    private Function<ApiEntityHolderSource, UserSoundsItem> holderSourceToUserSoundItems(
            final int collectionType) {
        return new Function<ApiEntityHolderSource, UserSoundsItem>() {
            @Override
            public UserSoundsItem apply(ApiEntityHolderSource entityHolderSource) {
                final Optional<ApiEntityHolder> entityHolder = entityHolderSource.getEntityHolder();

                if (entityHolder.isPresent()) {
                    final PropertySet properties = entityHolder.get().toPropertySet();

                    if (properties.get(EntityProperty.URN).isTrack()) {
                        return UserSoundsItem.fromTrackItem(TrackItem.from(properties), collectionType);
                    } else {
                        return UserSoundsItem.fromPlaylistItem(PlaylistItem.from(properties), collectionType);
                    }
                }

                return null;
            }
        };
    }

}
