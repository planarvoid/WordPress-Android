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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.functions.Func1;

import static com.soundcloud.android.profile.UserSoundsItem.fromPlaylistItem;
import static com.soundcloud.android.profile.UserSoundsItem.fromTrackItem;

public class UserSoundsMapper implements Func1<UserProfileRecord, Iterable<UserSoundsItem>> {

    private final EntityHolderSourceMapper entityHolderSourceMapper;
    private final EntityHolderMapper entityHolderMapper;

    @Inject
    public UserSoundsMapper(EntityHolderSourceMapper entityHolderSourceMapper, EntityHolderMapper entityHolderMapper) {
        this.entityHolderSourceMapper = entityHolderSourceMapper;
        this.entityHolderMapper = entityHolderMapper;
    }

    @Override
    public Iterable<UserSoundsItem> call(UserProfileRecord userProfile) {
        final List<UserSoundsItem> items = new ArrayList<>();

        items.addAll(entityHolderSourceMapper.map(UserSoundsTypes.SPOTLIGHT, userProfile.getSpotlight()));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.TRACKS, userProfile.getTracks()));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.RELEASES, userProfile.getReleases()));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.PLAYLISTS, userProfile.getPlaylists()));
        items.addAll(entityHolderSourceMapper.map(UserSoundsTypes.REPOSTS, userProfile.getReposts()));
        items.addAll(entityHolderSourceMapper.map(UserSoundsTypes.LIKES, userProfile.getLikes()));

        return items;
    }

    public static class EntityHolderSourceMapper {

        @Inject
        public EntityHolderSourceMapper() {
        }

        public List<UserSoundsItem> map(
                int collectionType,
                ModelCollection<? extends ApiEntityHolderSource> itemsToMap) {
            final List<UserSoundsItem> items = new ArrayList<>();

            if (!itemsToMap.getCollection().isEmpty()) {
                items.add(UserSoundsItem.fromHeader(collectionType));

                items.addAll(Lists.transform(
                        itemsToMap.getCollection(),
                        toUserSoundsItem(collectionType)));

                if (itemsToMap.getNextLink().isPresent()) {
                    items.add(UserSoundsItem.fromViewAll(collectionType));
                }

                items.add(UserSoundsItem.fromDivider());
            }
            return items;
        }

        private Function<ApiEntityHolderSource, UserSoundsItem> toUserSoundsItem(
                final int collectionType) {
            return new Function<ApiEntityHolderSource, UserSoundsItem>() {
                @Override
                public UserSoundsItem apply(ApiEntityHolderSource entityHolderSource) {
                    final Optional<ApiEntityHolder> entityHolder = entityHolderSource
                            .getEntityHolder();

                    if (entityHolder.isPresent()) {
                        final PropertySet properties = entityHolder.get().toPropertySet();

                        if (properties.get(EntityProperty.URN).isTrack()) {
                            return fromTrackItem(TrackItem.from(properties), collectionType);
                        } else {
                            return fromPlaylistItem(PlaylistItem.from(properties), collectionType);
                        }
                    }

                    return null;
                }
            };
        }
    }

    public static class EntityHolderMapper {

        @Inject
        public EntityHolderMapper() {
        }

        public List<UserSoundsItem> map(
                int collectionType,
                ModelCollection<? extends ApiEntityHolder> itemsToMap) {
            final List<UserSoundsItem> items = new ArrayList<>();

            if (!itemsToMap.getCollection().isEmpty()) {
                items.add(UserSoundsItem.fromHeader(collectionType));

                items.addAll(Lists.transform(
                        itemsToMap.getCollection(),
                        toUserSoundsItem(collectionType)));

                if (itemsToMap.getNextLink().isPresent()) {
                    items.add(UserSoundsItem.fromViewAll(collectionType));
                }

                items.add(UserSoundsItem.fromDivider());
            }
            return items;
        }

        private Function<ApiEntityHolder, UserSoundsItem> toUserSoundsItem(
                final int collectionType) {
            return new Function<ApiEntityHolder, UserSoundsItem>() {
                @Override
                public UserSoundsItem apply(ApiEntityHolder holder) {
                    final PropertySet properties = holder.toPropertySet();

                    if (properties.get(EntityProperty.URN).isTrack()) {
                        return fromTrackItem(TrackItem.from(properties), collectionType);
                    } else {
                        return fromPlaylistItem(PlaylistItem.from(properties), collectionType);
                    }
                }
            };
        }
    }
}
