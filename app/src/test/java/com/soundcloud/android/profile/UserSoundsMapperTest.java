package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserSoundsMapperTest extends AndroidUnitTest {

    @Mock UserSoundsMapper.EntityHolderMapper entityHolderMapper;
    @Mock UserSoundsItem mockUserSoundsItem;

    @Test
    public void shouldMapItemsToUserSoundsItems() throws Exception {
        final ApiUserProfile profile = new UserProfileFixtures.Builder().populateAllCollections().build();

        mockEntityHolderMapper(UserSoundsTypes.SPOTLIGHT, toApiEntityHolderCollection(profile.getSpotlight()));
        mockEntityHolderMapper(UserSoundsTypes.TRACKS, profile.getTracks());
        mockEntityHolderMapper(UserSoundsTypes.RELEASES, profile.getReleases());
        mockEntityHolderMapper(UserSoundsTypes.PLAYLISTS, profile.getPlaylists());
        mockEntityHolderMapper(UserSoundsTypes.REPOSTS, toApiEntityHolderCollection(profile.getReposts()));
        mockEntityHolderMapper(UserSoundsTypes.LIKES, toApiEntityHolderCollection(profile.getLikes()));

        ArrayList<UserSoundsItem> result = newArrayList(new UserSoundsMapper(entityHolderMapper).call(profile));

        assertThat(result.size()).isEqualTo(6);
        for (UserSoundsItem item : result) assertThat(item).isEqualTo(mockUserSoundsItem);
    }

    @Test
    public void shouldMapEntityHolder() throws Exception {
        final ApiTrackPost trackPost = new ApiTrackPost(ModelFixtures.create(ApiTrack.class));
        final ApiPlaylistPost playlistPost = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class));
        final ModelCollection<ApiEntityHolder> tracks = new ModelCollection<>(newArrayList(trackPost, playlistPost));

        List<UserSoundsItem> result = new UserSoundsMapper.EntityHolderMapper().map(UserSoundsTypes.TRACKS, tracks);

        assertThat(result.size()).isEqualTo(4);
        assertThat(result.get(0).getItemType()).isEqualTo(UserSoundsItem.TYPE_DIVIDER);

        assertThat(result.get(1).getItemType()).isEqualTo(UserSoundsItem.TYPE_HEADER);
        assertThat(result.get(1).getCollectionType()).isEqualTo(UserSoundsTypes.TRACKS);

        assertThat(result.get(2).getItemType()).isEqualTo(UserSoundsItem.TYPE_TRACK);
        assertThat(result.get(2).getEntityUrn())
                .isEqualTo(trackPost.toPropertySet().get(EntityProperty.URN));

        assertThat(result.get(3).getItemType()).isEqualTo(UserSoundsItem.TYPE_PLAYLIST);
        assertThat(result.get(3).getEntityUrn())
                .isEqualTo(playlistPost.toPropertySet().get(EntityProperty.URN));
    }

    @Test
    public void shouldMapEntityHolderWithNextLink() throws Exception {
        final ApiTrackPost trackPost = new ApiTrackPost(ModelFixtures.create(ApiTrack.class));
        final Map<String, Link> links = new HashMap<>();
        links.put(ModelCollection.NEXT_LINK_REL, new Link("some://link"));

        final ModelCollection<ApiTrackPost> tracks = new ModelCollection<>(Collections.singletonList(trackPost), links);

        List<UserSoundsItem> result = new UserSoundsMapper.EntityHolderMapper().map(UserSoundsTypes.TRACKS, tracks);

        assertThat(result.get(3).getItemType()).isEqualTo(UserSoundsItem.TYPE_VIEW_ALL);
    }

    private ModelCollection<ApiEntityHolder> toApiEntityHolderCollection(
            ModelCollection<? extends ApiEntityHolderSource> spotlight) {
        ApiEntityHolder apiEntityHolder = spotlight.getCollection().get(0).getEntityHolder().get();
        ArrayList<ApiEntityHolder> apiEntityHolders = newArrayList(apiEntityHolder);
        return new ModelCollection<>(apiEntityHolders, spotlight.getLinks());
    }

    private void mockEntityHolderMapper(int collectionType, ModelCollection<? extends ApiEntityHolder> collection) {
        when(entityHolderMapper.map(collectionType, collection)).thenReturn(newArrayList(mockUserSoundsItem));
    }
}
