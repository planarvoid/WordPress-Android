package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.assertj.core.util.Lists;
import org.junit.Test;

import android.support.annotation.NonNull;

import java.util.List;

public class UserSoundsItemTest {

    @Test
    public void getsItemPositionAndFiltersDifferentModules() throws Exception {

        final UserSoundsItem itemInTracksModule = createUserSoundsItem(UserSoundsTypes.TRACKS, UserSoundsItem.TYPE_TRACK);
        final UserSoundsItem headerItemInAlbumsModule = createUserSoundsItem(UserSoundsTypes.ALBUMS, UserSoundsItem.TYPE_HEADER);
        final UserSoundsItem firstItem = createUserSoundsItem(UserSoundsTypes.ALBUMS, UserSoundsItem.TYPE_PLAYLIST);
        final UserSoundsItem secondClickedItem = createUserSoundsItem(UserSoundsTypes.ALBUMS, UserSoundsItem.TYPE_PLAYLIST);
        final UserSoundsItem itemInPlaylistsModule = createUserSoundsItem(UserSoundsTypes.PLAYLISTS, UserSoundsItem.TYPE_PLAYLIST);

        final List<UserSoundsItem> userSoundsItems = Lists.newArrayList(itemInTracksModule,
                                                                        headerItemInAlbumsModule,
                                                                        firstItem,
                                                                        secondClickedItem,
                                                                        itemInPlaylistsModule);
        int positionInModule = UserSoundsItem.getPositionInModule(userSoundsItems,
                                                                  secondClickedItem);

        assertThat(positionInModule).isEqualTo(1);
    }

    @NonNull
    private UserSoundsItem createUserSoundsItem(int collectionType, int itemType) {
        final UserSoundsItem item = mock(UserSoundsItem.class);
        when(item.collectionType()).thenReturn(collectionType);
        when(item.itemType()).thenReturn(itemType);
        return item;
    }
}
