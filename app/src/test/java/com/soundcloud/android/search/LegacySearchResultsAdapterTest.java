package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.UserItemPresenter;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LegacySearchResultsAdapterTest {
    @Mock private UserItemPresenter userPresenter;
    @Mock private TrackItemPresenter trackPresenter;
    @Mock private PlaylistItemPresenter playlistPresenter;
    @Mock private FollowingOperations followingOperations;
    @Mock private ViewGroup itemView;

    @Captor private ArgumentCaptor<List<PropertySet>> propSetCaptor;

    private TestEventBus eventBus = new TestEventBus();

    private LegacySearchResultsAdapter adapter;

    @Before
    public void setup() {
        adapter = new LegacySearchResultsAdapter(userPresenter, trackPresenter, playlistPresenter, followingOperations, eventBus);
    }

    @Test
    public void shouldDifferentiateItemViewTypes() {
        adapter.addItem(new PublicApiUser());
        adapter.addItem(new PublicApiTrack());
        adapter.addItem(new PublicApiPlaylist());

        expect(adapter.getItemViewType(0)).toEqual(LegacySearchResultsAdapter.TYPE_USER);
        expect(adapter.getItemViewType(1)).toEqual(LegacySearchResultsAdapter.TYPE_TRACK);
        expect(adapter.getItemViewType(2)).toEqual(LegacySearchResultsAdapter.TYPE_PLAYLIST);
    }

    @Test
    public void trackChangedForNewQueueEventShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(playingTrack));
        verify(trackPresenter).setPlayingTrack(playingTrack);
    }

    @Test
    public void trackChangedForPositionChangedEventShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack));
        verify(trackPresenter).setPlayingTrack(playingTrack);
    }

    @Test
    public void playableChangedEventShouldUpdateAdapterToReflectTheLatestLikeStatus() throws CreateModelException {
        final PublicApiPlaylist unlikedPlaylist = ModelFixtures.create(PublicApiPlaylist.class);
        unlikedPlaylist.user_like = false;

        adapter.addItem(unlikedPlaylist);

        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED,
                PlayableUpdatedEvent.forLike(Urn.forPlaylist(unlikedPlaylist.getId()), true, 1));
        adapter.getView(0, itemView, new FrameLayout(Robolectric.application));

        verify(playlistPresenter).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        expect(propSetCaptor.getValue().get(0).get(PlayableProperty.IS_LIKED)).toBeTrue();
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroyView() {
        adapter.onViewCreated();
        adapter.onDestroyView();
        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldRegisterItselfWithUserPresenterAsToggleFollowListenerInConstructor() {
        verify(userPresenter).setToggleFollowListener(adapter);
    }

    @Test
    public void subscribesToFollowObservableWhenToggleFollowClicked() {
        final PublicApiUser user = new PublicApiUser(123);
        adapter.addItem(user);
        final TestObservables.MockObservable<Boolean> observable = TestObservables.emptyObservable();
        when(followingOperations.toggleFollowing(user)).thenReturn(observable);

        ToggleButton toggleButton = new ToggleButton(Robolectric.application);
        adapter.onToggleFollowClicked(0, toggleButton);
        expect(observable.subscribedTo()).toBeTrue();
    }

}
