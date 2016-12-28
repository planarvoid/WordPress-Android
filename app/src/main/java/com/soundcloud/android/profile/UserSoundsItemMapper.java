package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.UserSoundsItem.fromPlaylistItem;
import static com.soundcloud.android.profile.UserSoundsItem.fromTrackItem;
import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.functions.Function;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class UserSoundsItemMapper implements Func1<UserProfile, Iterable<UserSoundsItem>> {

    private final EntityHolderMapper entityHolderMapper;

    @Inject
    public UserSoundsItemMapper(EntityHolderMapper entityHolderMapper) {
        this.entityHolderMapper = entityHolderMapper;
    }

    @Override
    public Iterable<UserSoundsItem> call(UserProfile userProfile) {
        final List<UserSoundsItem> items = new ArrayList<>(estimateItemCount(userProfile));

        items.addAll(entityHolderMapper.map(UserSoundsTypes.SPOTLIGHT, userProfile.getSpotlight()));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.TRACKS, userProfile.getTracks()));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.ALBUMS, userProfile.getAlbums()));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.PLAYLISTS, userProfile.getPlaylists()));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.REPOSTS, userProfile.getReposts()));
        items.addAll(entityHolderMapper.map(UserSoundsTypes.LIKES, userProfile.getLikes()));

        if (!items.isEmpty()) {
            items.add(UserSoundsItem.fromEndOfListDivider());
        }

        return items;
    }

    private int estimateItemCount(UserProfile userProfile) {
        //We can guess this pretty accurately. So why now.
        return 3 + userProfile.getSpotlight().getCollection().size()
                + 3 + userProfile.getTracks().getCollection().size()
                + 3 + userProfile.getAlbums().getCollection().size()
                + 3 + userProfile.getPlaylists().getCollection().size()
                + 3 + userProfile.getReposts().getCollection().size()
                + 3 + userProfile.getLikes().getCollection().size();
    }

    public static class EntityHolderMapper {

        @Inject
        public EntityHolderMapper() {
        }

        public List<UserSoundsItem> map(
                int collectionType,
                ModelCollection<? extends PlayableItem> itemsToMap) {
            final List<UserSoundsItem> items = new ArrayList<>(3 + itemsToMap.getCollection().size());

            if (!itemsToMap.getCollection().isEmpty()) {
                items.add(UserSoundsItem.fromDivider());

                items.add(UserSoundsItem.fromHeader(collectionType));

                items.addAll(transform(
                        itemsToMap.getCollection(),
                        toUserSoundsItem(collectionType)));

                if (itemsToMap.getNextLink().isPresent()) {
                    items.add(UserSoundsItem.fromViewAll(collectionType));
                }
            }
            return items;
        }

        private Function<PlayableItem, UserSoundsItem> toUserSoundsItem(
                final int collectionType) {
            return playableItem -> {
                if (playableItem.getUrn().isTrack()) {
                    return fromTrackItem((TrackItem) playableItem, collectionType);
                } else {
                    return fromPlaylistItem((PlaylistItem) playableItem, collectionType);
                }
            };
        }
    }
}
