package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.UserSoundsItem.fromPlaylistItem;
import static com.soundcloud.android.profile.UserSoundsItem.fromTrackItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.collection.LoadPlaylistRepostStatuses;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.collection.LoadTrackLikedStatuses;
import com.soundcloud.android.collection.LoadTrackRepostStatuses;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

public class UserSoundsStatusMapperTest extends AndroidUnitTest {

    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private LoadPlaylistRepostStatuses loadPlaylistRepostStatuses;
    @Mock private LoadTrackLikedStatuses loadTrackLikedStatuses;
    @Mock private LoadTrackRepostStatuses loadTrackRepostStatuses;

    private UserSoundsStatusMapper subject;
    private PlaylistItem playlistItem;
    private TrackItem trackItem;
    private Map<Urn, PropertySet> statusMap;

    @Before
    public void setUp() throws Exception {
        subject = new UserSoundsStatusMapper(loadPlaylistLikedStatuses, loadPlaylistRepostStatuses,
                loadTrackLikedStatuses, loadTrackRepostStatuses);

        playlistItem = PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
        trackItem = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        statusMap = new HashMap<>();
    }

    @Test
    public void shouldUpdatePlaylistLikedStatusToTrue() throws Exception {
        statusMap.put(playlistItem.getEntityUrn(), PropertySet.from(PlayableProperty.IS_USER_LIKE.bind(true)));
        when(loadPlaylistLikedStatuses.call(anyListOf(PropertySet.class))).thenReturn(statusMap);

        subject.map(newArrayList(fromPlaylistItem(playlistItem, UserSoundsTypes.SPOTLIGHT)));

        assertThat(playlistItem.isLiked()).isTrue();
    }

    @Test
    public void shouldUpdatePlaylistLikedStatusToFalse() throws Exception {
        statusMap.put(playlistItem.getEntityUrn(), PropertySet.from(PlayableProperty.IS_USER_LIKE.bind(false)));
        when(loadPlaylistLikedStatuses.call(anyListOf(PropertySet.class))).thenReturn(statusMap);

        subject.map(newArrayList(fromPlaylistItem(playlistItem, UserSoundsTypes.SPOTLIGHT)));

        assertThat(playlistItem.isLiked()).isFalse();
    }

    @Test
    public void shouldUpdatePlaylistRepostStatusToTrue() throws Exception {
        statusMap.put(playlistItem.getEntityUrn(), PropertySet.from(PlayableProperty.IS_USER_REPOST.bind(true)));
        when(loadPlaylistRepostStatuses.call(anyListOf(PropertySet.class))).thenReturn(statusMap);

        subject.map(newArrayList(fromPlaylistItem(playlistItem, UserSoundsTypes.SPOTLIGHT)));

        assertThat(playlistItem.isReposted()).isTrue();
    }

    @Test
    public void shouldUpdatePlaylistRepostStatusToFalse() throws Exception {
        statusMap.put(playlistItem.getEntityUrn(), PropertySet.from(PlayableProperty.IS_USER_REPOST.bind(false)));
        when(loadPlaylistRepostStatuses.call(anyListOf(PropertySet.class))).thenReturn(statusMap);

        subject.map(newArrayList(fromPlaylistItem(playlistItem, UserSoundsTypes.SPOTLIGHT)));

        assertThat(playlistItem.isReposted()).isFalse();
    }

    @Test
    public void shouldUpdateTrackLikedStatusToTrue() throws Exception {
        statusMap.put(trackItem.getEntityUrn(), PropertySet.from(PlayableProperty.IS_USER_LIKE.bind(true)));
        when(loadTrackLikedStatuses.call(anyListOf(PropertySet.class))).thenReturn(statusMap);

        subject.map(newArrayList(fromTrackItem(trackItem, UserSoundsTypes.SPOTLIGHT)));

        assertThat(trackItem.isLiked()).isTrue();
    }

    @Test
    public void shouldUpdateTrackLikedStatusToFalse() throws Exception {
        statusMap.put(trackItem.getEntityUrn(), PropertySet.from(PlayableProperty.IS_USER_LIKE.bind(false)));
        when(loadTrackLikedStatuses.call(anyListOf(PropertySet.class))).thenReturn(statusMap);

        subject.map(newArrayList(fromTrackItem(trackItem, UserSoundsTypes.SPOTLIGHT)));

        assertThat(trackItem.isLiked()).isFalse();
    }

    @Test
    public void shouldUpdateTrackRepostStatusToTrue() throws Exception {
        statusMap.put(trackItem.getEntityUrn(), PropertySet.from(PlayableProperty.IS_USER_REPOST.bind(true)));
        when(loadTrackRepostStatuses.call(anyListOf(PropertySet.class))).thenReturn(statusMap);

        subject.map(newArrayList(fromTrackItem(trackItem, UserSoundsTypes.SPOTLIGHT)));

        assertThat(trackItem.isReposted()).isTrue();
    }

    @Test
    public void shouldUpdateTrackRepostStatusToFalse() throws Exception {
        statusMap.put(trackItem.getEntityUrn(), PropertySet.from(PlayableProperty.IS_USER_REPOST.bind(false)));
        when(loadTrackRepostStatuses.call(anyListOf(PropertySet.class))).thenReturn(statusMap);

        subject.map(newArrayList(fromTrackItem(trackItem, UserSoundsTypes.SPOTLIGHT)));

        assertThat(trackItem.isReposted()).isFalse();
    }
}
