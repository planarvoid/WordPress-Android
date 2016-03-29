package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.UserSoundsItem.fromPlaylistItem;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.view.View;

public class UserSoundsItemClickListenerTest extends AndroidUnitTest {
    private static final Urn USER_URN = Urn.forUser(123L);

    @Mock private Navigator navigator;
    @Mock private View view;
    @Mock private Context context;

    private UserSoundsItemClickListener subject;

    @Before
    public void setUp() throws Exception {
        subject = new UserSoundsItemClickListener(navigator);
        when(view.getContext()).thenReturn(context);
    }

    @Test
    public void shouldOpenPlaylist() throws Exception {
        PlaylistItem playlistItem = PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
        subject.onItemClick(view, fromPlaylistItem(playlistItem, UserSoundsTypes.SPOTLIGHT), null, null);

        verify(navigator).openPlaylist(context, playlistItem.getUrn(), Screen.PROFILE_SOUNDS_PLAYLIST);
    }

    @Test
    public void shouldOpenReposts() throws Exception {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L));
        subject.onItemClick(view, UserSoundsItem.fromViewAll(UserSoundsTypes.REPOSTS), USER_URN, searchSourceInfo);

        verify(navigator).openReposts(context, USER_URN, Screen.USERS_REPOSTS, searchSourceInfo);
    }

    @Test
    public void shouldOpenTracks() throws Exception {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L));
        subject.onItemClick(view, UserSoundsItem.fromViewAll(UserSoundsTypes.TRACKS), USER_URN, searchSourceInfo);

        verify(navigator).openProfileTracks(context, USER_URN, Screen.USER_TRACKS, searchSourceInfo);
    }
}
