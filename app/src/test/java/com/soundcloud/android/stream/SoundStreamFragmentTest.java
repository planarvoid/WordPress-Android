package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.RxTestHelper.pagerWithNextPage;
import static com.soundcloud.android.rx.TestObservables.withSubscription;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamFragmentTest {

    private FragmentActivity activity = new FragmentActivity();

    private SoundStreamFragment fragment;

    @Mock private SoundStreamOperations soundStreamOperations;
    @Mock private SoundStreamAdapter adapter;
    @Mock private ListViewController listViewController;
    @Mock private PullToRefreshController pullToRefreshController;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private Subscription subscription;
    @Mock private EmptyView emptyView;

    @Before
    public void setup() {
        Observable<List<PropertySet>> streamItems = withSubscription(subscription, just(Collections.<PropertySet>emptyList()));
        when(soundStreamOperations.pager()).thenReturn(RxTestHelper.<List<PropertySet>>pagerWithSinglePage());
        when(soundStreamOperations.existingStreamItems()).thenReturn(streamItems);
        when(soundStreamOperations.updatedStreamItems()).thenReturn(streamItems);
        when(listViewController.getEmptyView()).thenReturn(emptyView);
        when(playbackOperations.playTracks(any(Observable.class), any(Urn.class), anyInt(), any(PlaySessionSource.class))).thenReturn(Observable.<List<Urn>>empty());
        fragment = new SoundStreamFragment(soundStreamOperations, adapter, listViewController, pullToRefreshController,
                playbackOperations, TestSubscribers.expandPlayerSubscriber());
        fragment.onAttach(activity);
    }

    @Test
    public void shouldRequestAvailableSoundStreamItemsWhenCreated() {
        createFragment();
        verify(soundStreamOperations).existingStreamItems();
        verify(adapter).onCompleted();
    }

    @Test
    public void initialSoundStreamObservableShouldBePaged() {
        when(soundStreamOperations.pager()).thenReturn(pagerWithNextPage(Observable.<List<PropertySet>>never()));
        fragment.buildObservable().connect();
        expect(soundStreamOperations.pager().hasNext()).toBeTrue();
    }

    @Test
    public void refreshObservableShouldUpdateStreamItems() {
        fragment.refreshObservable().connect();
        verify(soundStreamOperations).updatedStreamItems();
        verify(adapter).onCompleted();
    }

    @Test
    public void refreshObservableShouldBePaged() {
        when(soundStreamOperations.pager()).thenReturn(pagerWithNextPage(Observable.<List<PropertySet>>never()));
        fragment.refreshObservable().connect();
        expect(soundStreamOperations.pager().hasNext()).toBeTrue();
    }

    @Test
    public void shouldConnectToListViewControllerInOnViewCreated() {
        createFragment();
        createFragmentView();
        verify(listViewController).connect(same(fragment), isA(ConnectableObservable.class));
    }

    @Test
    public void shouldConnectToPTRControllerInOnViewCreated() {
        createFragment();
        createFragmentView();
        verify(pullToRefreshController).connect(isA(ConnectableObservable.class), same(adapter));
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldUpdateLastSeenOnResume() {
        fragment.onCreate(null);
        fragment.onResume();
        verify(soundStreamOperations).updateLastSeen();
    }

    @Test
    public void shouldPlayTrackWhenClickingOnTrackItem() {
        Robolectric.shadowOf(fragment).setActivity(activity);
        final Observable<Urn> streamTracks = just((Urn.forTrack(123)));
        when(soundStreamOperations.trackUrnsForPlayback()).thenReturn(streamTracks);
        when(adapter.getItem(0)).thenReturn(PropertySet.from(PlayableProperty.URN.bind(Urn.forTrack(123))));
        fragment.onItemClick(null, null, 0, -1);

        verify(playbackOperations).playTracks(
                eq(streamTracks),
                eq(Urn.forTrack(123)),
                eq(0),
                eq(new PlaySessionSource(Screen.SIDE_MENU_STREAM)));
    }

    @Test
    public void shouldOpenPlaylistScreenWhenClickingOnPlaylistItem() {
        Robolectric.shadowOf(fragment).setActivity(activity);
        when(adapter.getItem(0)).thenReturn(PropertySet.from(PlayableProperty.URN.bind(Urn.forPlaylist(123))));
        fragment.onItemClick(null, null, 0, -1);

        final Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getParcelableExtra(PlaylistDetailActivity.EXTRA_URN)).toEqual(Urn.forPlaylist(123));
    }

    @Test
    public void shouldConfigureEmptyViewImage() {
        createFragment();
        createFragmentView();
        verify(emptyView).setImage(R.drawable.empty_stream);
    }

    @Test
    public void shouldConfigureDefaultEmptyViewIfOnboardingSucceeded() {
        createFragment();
        fragment.getArguments().putBoolean(SoundStreamFragment.ONBOARDING_RESULT_EXTRA, true);
        createFragmentView();
        verify(emptyView).setMessageText(R.string.list_empty_stream_message);
        verify(emptyView).setActionText(R.string.list_empty_stream_action);
    }

    @Test
    public void shouldConfigureErrorEmptyViewIfOnboardingFails() {
        createFragment();
        fragment.getArguments().putBoolean(SoundStreamFragment.ONBOARDING_RESULT_EXTRA, false);
        createFragmentView();
        verify(emptyView).setMessageText(R.string.error_onboarding_fail);
    }

    private void createFragment() {
        fragment.setArguments(new Bundle());
        Robolectric.shadowOf(fragment).setActivity(activity);
        fragment.onCreate(null);
    }

    private View createFragmentView() {
        Robolectric.shadowOf(fragment).setAttached(true);
        View view = fragment.onCreateView(activity.getLayoutInflater(), new FrameLayout(activity), null);
        Robolectric.shadowOf(fragment).setView(view);
        fragment.onViewCreated(view, null);
        return view;
    }
}