package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.UserSoundsItem.fromPlaylistItem;
import static com.soundcloud.android.profile.UserSoundsItem.fromTrackItem;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.view.View;

public class UserSoundsItemClickListenerTest extends AndroidUnitTest {
    private static final Urn USER_URN = Urn.forUser(123L);

    @Mock private Navigator navigator;
    @Mock private MixedItemClickListener mixedItemClickListener;
    @Mock private View view;
    @Mock private Context context;
    @Mock private Module module;

    private UserSoundsItemClickListener subject;

    @Before
    public void setUp() throws Exception {
        subject = new UserSoundsItemClickListener(navigator, mixedItemClickListener);
        when(view.getContext()).thenReturn(context);
    }

    @Test
    public void shouldDelegateTrackClicksToMixedItemClickListener() throws Exception {
        TrackItem trackItem = ModelFixtures.trackItem();
        subject.onItemClick(null,
                            view,
                            0,
                            fromTrackItem(trackItem, UserSoundsTypes.SPOTLIGHT),
                            USER_URN,
                            null,
                            module);

        verify(mixedItemClickListener).onProfilePostClick(null, view, 0, trackItem, USER_URN);
    }

    @Test
    public void shouldDelegatePlaylistClicksToMixedItemClickListener() throws Exception {
        PlaylistItem playlistItem = ModelFixtures.playlistItem();
        subject.onItemClick(null,
                            view,
                            0,
                            fromPlaylistItem(playlistItem, UserSoundsTypes.SPOTLIGHT),
                            null,
                            null,
                            module);

        verify(mixedItemClickListener).onPostClick(null, view, 0, playlistItem, module);
    }

    @Test
    public void shouldOpenReposts() throws Exception {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
        subject.onItemClick(null,
                            view,
                            0,
                            UserSoundsItem.fromViewAll(UserSoundsTypes.REPOSTS),
                            USER_URN,
                            searchSourceInfo,
                            module);

        verify(navigator).openProfileReposts(context, USER_URN, Screen.USERS_REPOSTS, searchSourceInfo);
    }

    @Test
    public void shouldOpenTracks() throws Exception {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
        subject.onItemClick(null,
                            view,
                            0,
                            UserSoundsItem.fromViewAll(UserSoundsTypes.TRACKS),
                            USER_URN,
                            searchSourceInfo,
                            module);

        verify(navigator).openProfileTracks(context, USER_URN, Screen.USER_TRACKS, searchSourceInfo);
    }

    @Test
    public void shouldOpenAlbums() throws Exception {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
        subject.onItemClick(null,
                            view,
                            0,
                            UserSoundsItem.fromViewAll(UserSoundsTypes.ALBUMS),
                            USER_URN,
                            searchSourceInfo,
                            module);

        verify(navigator).openProfileAlbums(context, USER_URN, Screen.USER_TRACKS, searchSourceInfo);
    }

    @Test
    public void shouldOpenLikes() throws Exception {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
        subject.onItemClick(null,
                            view,
                            0,
                            UserSoundsItem.fromViewAll(UserSoundsTypes.LIKES),
                            USER_URN,
                            searchSourceInfo,
                            module);

        verify(navigator).openProfileLikes(context, USER_URN, Screen.USER_LIKES, searchSourceInfo);
    }

    @Test
    public void shouldOpenPlaylists() throws Exception {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
        subject.onItemClick(null,
                            view,
                            0,
                            UserSoundsItem.fromViewAll(UserSoundsTypes.PLAYLISTS),
                            USER_URN,
                            searchSourceInfo,
                            module);

        verify(navigator).openProfilePlaylists(context, USER_URN, Screen.USER_PLAYLISTS, searchSourceInfo);
    }

    @Test
    public void shouldOpenPlaylist() throws Exception {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
        final UserSoundsItem item = UserSoundsItem.fromPlaylistItem(playlistItem, UserSoundsTypes.PLAYLISTS);
        final Module module = Module.create(Module.USER_PLAYLISTS, 1);
        final int position = 0;

        subject.onItemClick(null,
                            view,
                            position,
                            item,
                            USER_URN,
                            searchSourceInfo,
                            module);

        verify(mixedItemClickListener).onPostClick(null, view, position, playlistItem, module);
    }
}
