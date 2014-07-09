package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.android.OperatorPaged;
import rx.observables.ConnectableObservable;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamFragmentTest {

    private FragmentActivity activity = new FragmentActivity();
    private ConnectableObservable<OperatorPaged.Page<List<PropertySet>>> streamItems;

    @InjectMocks
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
        streamItems = TestObservables.emptyConnectableObservable(subscription);
        when(soundStreamOperations.existingStreamItems()).thenReturn(streamItems);
        when(soundStreamOperations.updatedStreamItems()).thenReturn(streamItems);
        when(listViewController.getEmptyView()).thenReturn(emptyView);
    }

    @Test
    public void shouldRequestAvailableSoundStreamItemsWhenCreated() {
        createFragment();
        verify(soundStreamOperations).existingStreamItems();
        verify(adapter).onCompleted();
    }

    @Test
    public void shouldAttachListViewControllerInOnViewCreated() {
        createFragment();
        fragment.connectObservable(streamItems);
        createFragmentView();
        verify(listViewController).onViewCreated(refEq(fragment), refEq(streamItems),
                refEq(fragment.getView()), refEq(adapter), refEq(adapter));
    }

    @Test
    public void shouldAttachPullToRefreshControllerInOnViewCreated() {
        createFragment();
        fragment.connectObservable(streamItems);
        createFragmentView();
        verify(pullToRefreshController).onViewCreated(fragment, streamItems, adapter);
    }

    @Test
    public void refreshObservableShouldUpdateStreamItems() {
        fragment.refreshObservable();
        verify(soundStreamOperations).updatedStreamItems();
    }

    @Test
    public void shouldForwardOnViewCreatedEventToAdapter() {
        createFragment();
        createFragmentView();
        verify(adapter).onViewCreated();
    }

    @Test
    public void shouldForwardOnDestroyViewEventToAdapter() {
        fragment.onDestroyView();
        verify(adapter).onDestroyView();
    }

    @Test
    public void shouldDetachPullToRefreshControllerOnDestroyView() {
        fragment.onDestroyView();
        verify(pullToRefreshController).onDestroyView();
    }

    @Test
    public void shouldDetachListViewControllerOnDestroyView() {
        fragment.onDestroyView();
        verify(listViewController).onDestroyView();
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldUpdateLastSeenOnResume() {
        fragment.onResume();
        verify(soundStreamOperations).updateLastSeen();
    }

    @Test
    public void shouldPlayTrackWhenClickingOnTrackItem() {
        Robolectric.shadowOf(fragment).setActivity(activity);
        final Observable<TrackUrn> streamTracks = Observable.just((Urn.forTrack(123)));
        when(soundStreamOperations.trackUrnsForPlayback()).thenReturn(streamTracks);
        when(adapter.getItem(0)).thenReturn(PropertySet.from(PlayableProperty.URN.bind(Urn.forTrack(123))));
        fragment.onItemClick(null, null, 0, -1);

        verify(playbackOperations).playTracks(activity, Urn.forTrack(123), streamTracks, 0, Screen.SIDE_MENU_STREAM);
    }

    @Test
    public void shouldOpenPlaylistScreenWhenClickingOnPlaylistItem() {
        Robolectric.shadowOf(fragment).setActivity(activity);
        when(adapter.getItem(0)).thenReturn(PropertySet.from(PlayableProperty.URN.bind(Urn.forPlaylist(123))));
        fragment.onItemClick(null, null, 0, -1);

        final Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getParcelableExtra(Playlist.EXTRA_URN)).toEqual(Urn.forPlaylist(123));
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