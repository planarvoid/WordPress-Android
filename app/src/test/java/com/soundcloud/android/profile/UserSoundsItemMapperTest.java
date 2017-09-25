package com.soundcloud.android.profile;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class UserSoundsItemMapperTest {

    @Mock UserSoundsItemMapper.EntityHolderMapper entityHolderMapper;
    @Mock UserSoundsItem mockUserSoundsItem;

    @Test
    public void shouldMapItemsToUserSoundsItems() throws Exception {
        UserProfile profile = new UserProfileFixtures.Builder().populateAllCollections().build();

        mockEntityHolderMapper(profile, UserSoundsTypes.SPOTLIGHT, profile.getSpotlight());
        mockEntityHolderMapper(profile, UserSoundsTypes.TRACKS, profile.getTracks());
        mockEntityHolderMapper(profile, UserSoundsTypes.ALBUMS, profile.getAlbums());
        mockEntityHolderMapper(profile, UserSoundsTypes.PLAYLISTS, profile.getPlaylists());
        mockEntityHolderMapper(profile, UserSoundsTypes.REPOSTS, profile.getReposts());
        mockEntityHolderMapper(profile, UserSoundsTypes.LIKES, profile.getLikes());

        ArrayList<UserSoundsItem> result = newArrayList(new UserSoundsItemMapper(entityHolderMapper).call(profile));

        assertThat(result.size()).isEqualTo(7);

        assertThat(result.get(0)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(1)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(2)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(3)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(4)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(5)).isEqualTo(mockUserSoundsItem);
        assertThat(result.get(6).itemType()).isEqualTo(UserSoundsItem.TYPE_END_OF_LIST_DIVIDER);
    }

    @Test
    public void shouldMapEntityHolder() throws Exception {
        final UserProfile profile = new UserProfileFixtures.Builder().populateAllCollections().build();
        final TrackItem trackPost = ModelFixtures.trackItem();
        final PlaylistItem playlistPost = ModelFixtures.playlistItem();
        final ModelCollection<PlayableItem> tracks = new ModelCollection<>(newArrayList(trackPost, playlistPost));

        List<UserSoundsItem> result = new UserSoundsItemMapper.EntityHolderMapper().map(profile, UserSoundsTypes.TRACKS, tracks);

        assertThat(result.size()).isEqualTo(4);
        assertThat(result.get(0).itemType()).isEqualTo(UserSoundsItem.TYPE_DIVIDER);

        assertThat(result.get(1).itemType()).isEqualTo(UserSoundsItem.TYPE_HEADER);
        assertThat(result.get(1).collectionType()).isEqualTo(UserSoundsTypes.TRACKS);

        assertThat(result.get(2).itemType()).isEqualTo(UserSoundsItem.TYPE_TRACK);
        assertThat(result.get(2).getUrn())
                .isEqualTo(trackPost.getUrn());

        assertThat(result.get(3).itemType()).isEqualTo(UserSoundsItem.TYPE_PLAYLIST);
        assertThat(result.get(3).getUrn())
                .isEqualTo(playlistPost.getUrn());
    }

    @Test
    public void shouldMapEntityHolderWithNextLink() throws Exception {
        final UserProfile profile = new UserProfileFixtures.Builder().populateAllCollections().build();
        final TrackItem trackItem = ModelFixtures.trackItem();
        final Map<String, Link> links = new HashMap<>();
        links.put(ModelCollection.NEXT_LINK_REL, new Link("some://link"));

        final ModelCollection<PlayableItem> tracks = new ModelCollection<>(
                singletonList(trackItem), links);

        List<UserSoundsItem> result = new UserSoundsItemMapper.EntityHolderMapper().map(profile, UserSoundsTypes.TRACKS, tracks);

        assertThat(result.get(3).itemType()).isEqualTo(UserSoundsItem.TYPE_VIEW_ALL);
    }

    private void mockEntityHolderMapper(UserProfile profile, int collectionType, ModelCollection<? extends PlayableItem> collection) {
        when(entityHolderMapper.map(profile, collectionType, collection)).thenReturn(newArrayList(mockUserSoundsItem));
    }
}
