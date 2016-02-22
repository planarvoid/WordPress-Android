package com.soundcloud.android.profile;

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
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserSoundsMapperTest extends AndroidUnitTest {

    private UserSoundsMapper subject;

    @Mock UserSoundsMapper.EntityHolderSourceMapper entityHolderSourceMapper;
    @Mock UserSoundsMapper.EntityHolderMapper entityHolderMapper;
    @Mock UserSoundsItem mockUserSoundsItem;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void shouldMapItemsToUserSoundsItems() throws Exception {
        final ApiPlayableSource track = ModelFixtures.apiTrackHolder();
        final ModelCollection<ApiPlayableSource> spotlight = new ModelCollection<>(singletonList(track));

        final ApiTrackPost trackPost = new ApiTrackPost(ModelFixtures.create(ApiTrack.class));
        final ModelCollection<ApiTrackPost> tracks = new ModelCollection<>(Collections.singletonList(trackPost));

        final ApiPlaylistPost release = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class));
        final ModelCollection<ApiPlaylistPost> releases = new ModelCollection<>(Collections.singletonList(release));

        final ApiPlaylistPost playlist = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class));
        final ModelCollection<ApiPlaylistPost> playlists = new ModelCollection<>(Collections.singletonList(playlist));

        final ApiPlayableSource trackRepost = ModelFixtures.apiTrackHolder();
        final ModelCollection<ApiPlayableSource> reposts = new ModelCollection<>(Collections.singletonList(trackRepost));

        final ApiPlayableSource trackLike = ModelFixtures.apiTrackHolder();
        final ModelCollection<ApiPlayableSource> likes = new ModelCollection<>(Collections.singletonList(trackLike));

        final ApiUserProfile profile = new UserProfileFixtures.Builder()
                .spotlight(spotlight)
                .tracks(tracks)
                .releases(releases)
                .playlists(playlists)
                .reposts(reposts)
                .likes(likes)
                .build();

        when(entityHolderSourceMapper.map(UserSoundsTypes.SPOTLIGHT, spotlight))
                .thenReturn(newArrayList(mockUserSoundsItem));
        when(entityHolderMapper.map(UserSoundsTypes.TRACKS, tracks)).thenReturn(newArrayList(mockUserSoundsItem));
        when(entityHolderMapper.map(UserSoundsTypes.RELEASES, releases)).thenReturn(newArrayList(mockUserSoundsItem));
        when(entityHolderMapper.map(UserSoundsTypes.PLAYLISTS, playlists)).thenReturn(newArrayList(mockUserSoundsItem));
        when(entityHolderSourceMapper.map(UserSoundsTypes.REPOSTS, reposts)).thenReturn(newArrayList(mockUserSoundsItem));
        when(entityHolderSourceMapper.map(UserSoundsTypes.LIKES, likes)).thenReturn(newArrayList(mockUserSoundsItem));

        ArrayList<UserSoundsItem> result = newArrayList(
                new UserSoundsMapper(entityHolderSourceMapper, entityHolderMapper).call(profile));

        assertThat(result.size()).isEqualTo(6);
        assertThat(result.get(0)).isEqualTo(mockUserSoundsItem);
    }

    @Test
    public void shouldMapEntityHolderSource() throws Exception {
        final ApiPlayableSource track = ModelFixtures.apiTrackHolder();
        final ApiPlayableSource playlist = ModelFixtures.apiPlaylistHolder();
        final ModelCollection<ApiPlayableSource> spotlight = new ModelCollection<>(newArrayList(track, playlist));

        List<UserSoundsItem> result = new UserSoundsMapper.EntityHolderSourceMapper()
                .map(UserSoundsTypes.SPOTLIGHT, spotlight);

        assertThat(result.size()).isEqualTo(4);
        assertThat(result.get(0).getItemType()).isEqualTo(UserSoundsItem.TYPE_HEADER);
        assertThat(result.get(0).getCollectionType()).isEqualTo(UserSoundsTypes.SPOTLIGHT);

        assertThat(result.get(1).getItemType()).isEqualTo(UserSoundsItem.TYPE_TRACK);
        assertThat(result.get(1).getEntityUrn())
                .isEqualTo(track.getEntityHolder().get().toPropertySet().get(EntityProperty.URN));

        assertThat(result.get(2).getItemType()).isEqualTo(UserSoundsItem.TYPE_PLAYLIST);
        assertThat(result.get(2).getEntityUrn())
                .isEqualTo(playlist.getEntityHolder().get().toPropertySet().get(EntityProperty.URN));

        assertThat(result.get(3).getItemType()).isEqualTo(UserSoundsItem.TYPE_DIVIDER);
    }

    @Test
    public void shouldMapEntityHolderSourceWithNextLink() throws Exception {
        final ApiPlayableSource track = ModelFixtures.apiTrackHolder();
        final Map<String, Link> links = new HashMap<>();
        links.put(ModelCollection.NEXT_LINK_REL, new Link("some://link"));
        final ModelCollection<ApiPlayableSource> spotlight = new ModelCollection<>(newArrayList(track), links);

        List<UserSoundsItem> result = new UserSoundsMapper.EntityHolderSourceMapper()
                .map(UserSoundsTypes.SPOTLIGHT, spotlight);

        assertThat(result.get(2).getItemType()).isEqualTo(UserSoundsItem.TYPE_VIEW_ALL);
    }

    @Test
    public void shouldMapEntityHolder() throws Exception {
        final ApiTrackPost trackPost = new ApiTrackPost(ModelFixtures.create(ApiTrack.class));
        final ModelCollection<ApiTrackPost> tracks = new ModelCollection<>(Collections.singletonList(trackPost));

        List<UserSoundsItem> result = new UserSoundsMapper.EntityHolderMapper().map(UserSoundsTypes.TRACKS, tracks);

        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0).getItemType()).isEqualTo(UserSoundsItem.TYPE_HEADER);
        assertThat(result.get(0).getCollectionType()).isEqualTo(UserSoundsTypes.TRACKS);

        assertThat(result.get(1).getItemType()).isEqualTo(UserSoundsItem.TYPE_TRACK);
        assertThat(result.get(1).getEntityUrn())
                .isEqualTo(trackPost.toPropertySet().get(EntityProperty.URN));

        assertThat(result.get(2).getItemType()).isEqualTo(UserSoundsItem.TYPE_DIVIDER);
    }

    @Test
    public void shouldMapEntityHolderWithNextLink() throws Exception {
        final ApiTrackPost trackPost = new ApiTrackPost(ModelFixtures.create(ApiTrack.class));
        final Map<String, Link> links = new HashMap<>();
        links.put(ModelCollection.NEXT_LINK_REL, new Link("some://link"));

        final ModelCollection<ApiTrackPost> tracks = new ModelCollection<>(Collections.singletonList(trackPost), links);

        List<UserSoundsItem> result = new UserSoundsMapper.EntityHolderMapper().map(UserSoundsTypes.TRACKS, tracks);

        assertThat(result.get(2).getItemType()).isEqualTo(UserSoundsItem.TYPE_VIEW_ALL);
    }
}