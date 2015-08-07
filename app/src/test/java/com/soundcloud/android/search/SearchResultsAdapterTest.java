package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchResultsAdapter.TYPE_PLAYLIST;
import static com.soundcloud.android.search.SearchResultsAdapter.TYPE_TRACK;
import static com.soundcloud.android.search.SearchResultsAdapter.TYPE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.view.adapters.FollowableUserItemRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.support.v4.app.Fragment;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.List;

public class SearchResultsAdapterTest extends AndroidUnitTest {

    @Mock private FollowableUserItemRenderer userRenderer;
    @Mock private TrackItemRenderer trackRenderer;
    @Mock private PlaylistItemRenderer playlistRenderer;
    @Mock private ViewGroup itemView;
    @Mock private Fragment fragment;

    @Captor private ArgumentCaptor<List<PlaylistItem>> playlistItemCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private SearchResultsAdapter adapter;

    @Before
    public void setup() {
        adapter = new SearchResultsAdapter(userRenderer, trackRenderer, playlistRenderer, eventBus);
    }

    @Test
    public void shouldDifferentiateItemViewTypesForUniversalSearchResult() {
        adapter.addItem(dummyUserItem());
        adapter.addItem(dummyTrackItem());
        adapter.addItem(dummyPlaylistItem());

        assertThat(adapter.getItemViewType(0)).isEqualTo(TYPE_USER);
        assertThat(adapter.getItemViewType(1)).isEqualTo(TYPE_TRACK);
        assertThat(adapter.getItemViewType(2)).isEqualTo(TYPE_PLAYLIST);
    }

    @Test
    public void shouldDifferentiateItemViewTypesForDifferentResultTypes() {
        adapter.addItem(dummyUserItem());
        adapter.addItem(dummyTrackItem());
        adapter.addItem(dummyPlaylistItem());

        assertThat(adapter.getItemViewType(0)).isEqualTo(TYPE_USER);
        assertThat(adapter.getItemViewType(1)).isEqualTo(TYPE_TRACK);
        assertThat(adapter.getItemViewType(2)).isEqualTo(TYPE_PLAYLIST);
    }

    @Test
    public void trackChangedForNewQueueEventShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated(fragment, null, null);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(playingTrack, Urn.NOT_SET, 0));

        verify(trackRenderer).setPlayingTrack(playingTrack);
    }

    @Test
    public void trackChangedForPositionChangedEventShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated(fragment, null, null);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack, Urn.NOT_SET, 0));
        verify(trackRenderer).setPlayingTrack(playingTrack);
    }

    @Test
    public void playableChangedEventShouldUpdateAdapterToReflectTheLatestLikeStatus() {
        PropertySet unlikedPlaylist = ModelFixtures.create(ApiPlaylist.class).toPropertySet();
        unlikedPlaylist.put(PlaylistProperty.IS_LIKED, false);
        adapter.addItem(dummyUserItem());
        adapter.addItem(dummyTrackItem());
        adapter.addItem(PlaylistItem.from(unlikedPlaylist));
        adapter.onViewCreated(fragment, null, null);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromLike(unlikedPlaylist.get(PlayableProperty.URN), true, 1));

        final int playlistPosition = 2;
        adapter.getView(playlistPosition, itemView, new FrameLayout(context()));

        verify(playlistRenderer).bindItemView(eq(playlistPosition), refEq(itemView), playlistItemCaptor.capture());
        assertThat(playlistItemCaptor.getValue().get(playlistPosition).isLiked()).isTrue();
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroyView() {
        adapter.onViewCreated(fragment, null, null);
        adapter.onDestroyView(fragment);
        eventBus.verifyUnsubscribed();
    }

    private UserItem dummyUserItem() {
        return UserItem.from(ApiUniversalSearchItem.forUser(ModelFixtures.create(ApiUser.class)).toPropertySet());
    }

    private TrackItem dummyTrackItem() {
        return TrackItem.from(ApiUniversalSearchItem.forTrack(ModelFixtures.create(ApiTrack.class)).toPropertySet());
    }

    private PlaylistItem dummyPlaylistItem() {
        return PlaylistItem.from(ApiUniversalSearchItem.forPlaylist(ModelFixtures.create(ApiPlaylist.class)).toPropertySet());
    }
}
