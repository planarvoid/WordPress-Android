package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserSoundsItemMapperTest extends AndroidUnitTest {

    @Mock UserSoundsItemMapper.EntityHolderMapper entityHolderMapper;
    @Mock UserSoundsItem mockUserSoundsItem;
    @Mock PlayQueueManager playQueueManager;

    @Test
    public void shouldMapItemsToUserSoundsItems() throws Exception {
        UserProfile profile = new UserProfileFixtures.Builder().populateAllCollections().build();

        mockEntityHolderMapper(UserSoundsTypes.SPOTLIGHT, profile.getSpotlight());
        mockEntityHolderMapper(UserSoundsTypes.TRACKS, profile.getTracks());
        mockEntityHolderMapper(UserSoundsTypes.ALBUMS, profile.getAlbums());
        mockEntityHolderMapper(UserSoundsTypes.PLAYLISTS, profile.getPlaylists());
        mockEntityHolderMapper(UserSoundsTypes.REPOSTS, profile.getReposts());
        mockEntityHolderMapper(UserSoundsTypes.LIKES, profile.getLikes());

        ArrayList<UserSoundsItem> result = newArrayList(new UserSoundsItemMapper(entityHolderMapper).call(profile));

        assertThat(result.size()).isEqualTo(7);

        assertThat(result.get(0)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(1)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(2)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(3)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(4)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(5)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(6).getItemType()).isEqualTo(UserSoundsItem.TYPE_END_OF_LIST_DIVIDER);
    }

    @Test
    public void shouldMapEntityHolder() throws Exception {
        final PropertySet trackPost = new ApiTrackPost(ModelFixtures.create(ApiTrack.class)).toPropertySet();
        final PropertySet playlistPost = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class)).toPropertySet();
        final ModelCollection<PropertySet> tracks = new ModelCollection<>(newArrayList(trackPost, playlistPost));

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(1L)));

        List<UserSoundsItem> result = new UserSoundsItemMapper.EntityHolderMapper().map(UserSoundsTypes.TRACKS, tracks);

        assertThat(result.size()).isEqualTo(4);
        assertThat(result.get(0).getItemType()).isEqualTo(UserSoundsItem.TYPE_DIVIDER);

        assertThat(result.get(1).getItemType()).isEqualTo(UserSoundsItem.TYPE_HEADER);
        assertThat(result.get(1).getCollectionType()).isEqualTo(UserSoundsTypes.TRACKS);

        assertThat(result.get(2).getItemType()).isEqualTo(UserSoundsItem.TYPE_TRACK);
        assertThat(result.get(2).getUrn())
                .isEqualTo(trackPost.get(EntityProperty.URN));

        assertThat(result.get(3).getItemType()).isEqualTo(UserSoundsItem.TYPE_PLAYLIST);
        assertThat(result.get(3).getUrn())
                .isEqualTo(playlistPost.get(EntityProperty.URN));
    }

    @Test
    public void shouldMapEntityHolderWithNextLink() throws Exception {
        final ApiTrackPost trackPost = new ApiTrackPost(ModelFixtures.create(ApiTrack.class));
        final Map<String, Link> links = new HashMap<>();
        links.put(ModelCollection.NEXT_LINK_REL, new Link("some://link"));

        final ModelCollection<PropertySet> tracks = new ModelCollection<>(
                singletonList(create(ApiTrack.class).toPropertySet()), links);

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(1L)));

        List<UserSoundsItem> result = new UserSoundsItemMapper.EntityHolderMapper().map(UserSoundsTypes.TRACKS, tracks);

        assertThat(result.get(3).getItemType()).isEqualTo(UserSoundsItem.TYPE_VIEW_ALL);
    }

    private void mockEntityHolderMapper(int collectionType, ModelCollection<PropertySet> collection) {
        when(entityHolderMapper.map(collectionType, collection)).thenReturn(newArrayList(mockUserSoundsItem));
    }
}
