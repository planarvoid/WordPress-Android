package com.soundcloud.android.collection;

import static com.soundcloud.android.model.PlayableProperty.IS_USER_LIKE;
import static com.soundcloud.android.model.PlayableProperty.IS_USER_REPOST;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

public class PlayableItemStatusLoaderTest extends AndroidUnitTest {

    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private LoadPlaylistRepostStatuses loadPlaylistRepostStatuses;
    @Mock private LoadTrackLikedStatuses loadTrackLikedStatuses;
    @Mock private LoadTrackRepostStatuses loadTrackRepostStatuses;

    private PlayableItemStatusLoader subject;
    private Map<Urn, PropertySet> statusMap;
    private PropertySet playlistPropertySet;
    private PropertySet trackPropertySet;

    @Before
    public void setUp() throws Exception {
        subject = new PlayableItemStatusLoader(loadPlaylistLikedStatuses, loadPlaylistRepostStatuses,
                                               loadTrackLikedStatuses, loadTrackRepostStatuses);

        playlistPropertySet = ModelFixtures.create(ApiPlaylist.class).toPropertySet();
        trackPropertySet = ModelFixtures.create(ApiTrack.class).toPropertySet();
        statusMap = new HashMap<>();
    }

    @Test
    public void shouldUpdatePlaylistLikedStatusToTrue() throws Exception {
        statusMap.put(playlistPropertySet.get(PlayableProperty.URN), PropertySet.from(IS_USER_LIKE.bind(true)));
        when(loadPlaylistLikedStatuses.call(anyCollection())).thenReturn(statusMap);

        subject.call(singletonList(playlistPropertySet));

        assertThat(playlistPropertySet.get(IS_USER_LIKE)).isTrue();
    }

    @Test
    public void shouldUpdatePlaylistLikedStatusToFalse() throws Exception {
        statusMap.put(playlistPropertySet.get(PlayableProperty.URN), PropertySet.from(IS_USER_LIKE.bind(false)));
        when(loadPlaylistLikedStatuses.call(anyCollection())).thenReturn(statusMap);

        subject.call(singletonList(playlistPropertySet));

        assertThat(playlistPropertySet.get(IS_USER_LIKE)).isFalse();
    }

    @Test
    public void shouldUpdatePlaylistRepostStatusToTrue() throws Exception {
        statusMap.put(playlistPropertySet.get(PlayableProperty.URN), PropertySet.from(IS_USER_REPOST.bind(true)));
        when(loadPlaylistRepostStatuses.call(anyCollection())).thenReturn(statusMap);

        subject.call(singletonList(playlistPropertySet));

        assertThat(playlistPropertySet.get(IS_USER_REPOST)).isTrue();
    }

    @Test
    public void shouldUpdatePlaylistRepostStatusToFalse() throws Exception {
        statusMap.put(playlistPropertySet.get(PlayableProperty.URN), PropertySet.from(IS_USER_REPOST.bind(false)));
        when(loadPlaylistRepostStatuses.call(anyCollection())).thenReturn(statusMap);

        subject.call(singletonList(playlistPropertySet));

        assertThat(playlistPropertySet.get(IS_USER_REPOST)).isFalse();
    }

    @Test
    public void shouldUpdateTrackLikedStatusToTrue() throws Exception {
        statusMap.put(trackPropertySet.get(PlayableProperty.URN), PropertySet.from(IS_USER_LIKE.bind(true)));
        when(loadTrackLikedStatuses.call(anyCollection())).thenReturn(statusMap);

        subject.call(singletonList(trackPropertySet));

        assertThat(trackPropertySet.get(IS_USER_LIKE)).isTrue();
    }

    @Test
    public void shouldUpdateTrackLikedStatusToFalse() throws Exception {
        statusMap.put(trackPropertySet.get(PlayableProperty.URN), PropertySet.from(IS_USER_LIKE.bind(false)));
        when(loadTrackLikedStatuses.call(anyCollection())).thenReturn(statusMap);

        subject.call(singletonList(trackPropertySet));

        assertThat(trackPropertySet.get(IS_USER_LIKE)).isFalse();
    }

    @Test
    public void shouldUpdateTrackRepostStatusToTrue() throws Exception {
        statusMap.put(trackPropertySet.get(PlayableProperty.URN), PropertySet.from(IS_USER_REPOST.bind(true)));
        when(loadTrackRepostStatuses.call(anyCollection())).thenReturn(statusMap);

        subject.call(singletonList(trackPropertySet));

        assertThat(trackPropertySet.get(IS_USER_REPOST)).isTrue();
    }

    @Test
    public void shouldUpdateTrackRepostStatusToFalse() throws Exception {
        statusMap.put(trackPropertySet.get(PlayableProperty.URN), PropertySet.from(IS_USER_REPOST.bind(false)));
        when(loadTrackRepostStatuses.call(anyCollection())).thenReturn(statusMap);

        subject.call(singletonList(trackPropertySet));

        assertThat(trackPropertySet.get(IS_USER_REPOST)).isFalse();
    }
}
