package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.UserSoundsItem.fromPlaylistItem;
import static com.soundcloud.android.profile.UserSoundsItem.fromTrackItem;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.MoreCollections.filter;
import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserSoundsMapper implements Func1<UserProfileRecord, Iterable<UserSoundsItem>> {

    private final EntityHolderMapper entityHolderMapper;

    @Inject
    public UserSoundsMapper(EntityHolderMapper entityHolderMapper) {
        this.entityHolderMapper = entityHolderMapper;
    }

    @Override
    public Iterable<UserSoundsItem> call(UserProfileRecord userProfile) {
        final List<UserSoundsItem> items = new ArrayList<>();

        items.addAll(entityHolderMapper.map(UserSoundsTypes.SPOTLIGHT,
                convertToApiEntityHolderCollection(userProfile.getSpotlight())));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.TRACKS, userProfile.getTracks()));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.RELEASES, userProfile.getReleases()));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.PLAYLISTS, userProfile.getPlaylists()));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.REPOSTS,
                convertToApiEntityHolderCollection(userProfile.getReposts())));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.LIKES,
                convertToApiEntityHolderCollection(userProfile.getLikes())));

        return items;
    }

    @VisibleForTesting
    static ModelCollection<ApiEntityHolder> convertToApiEntityHolderCollection(
            ModelCollection<? extends ApiEntityHolderSource> sources) {
        Collection<? extends ApiEntityHolderSource> apiEntityHolderSources = filter(sources.getCollection(),
                new Predicate<ApiEntityHolderSource>() {
                    @Override
                    public boolean apply(ApiEntityHolderSource input) {
                        return input.getEntityHolder().isPresent();
                    }
                });
        Collection<ApiEntityHolder> apiEntityHolders = transform(apiEntityHolderSources,
                new Function<ApiEntityHolderSource, ApiEntityHolder>() {
                    @Nullable
                    @Override
                    public ApiEntityHolder apply(ApiEntityHolderSource input) {
                        return input.getEntityHolder().get();
                    }
                });
        return new ModelCollection<>(newArrayList(apiEntityHolders), sources.getLinks());
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

                items.addAll(transform(
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
