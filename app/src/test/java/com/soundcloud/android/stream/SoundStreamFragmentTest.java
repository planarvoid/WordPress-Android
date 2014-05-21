package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.storage.PropertySet;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.view.ListViewController;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Subscription;
import rx.android.OperatorPaged;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.FrameLayout;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamFragmentTest {

    private FragmentActivity activity = new FragmentActivity();
    private ConnectableObservable<OperatorPaged.Page<List<PropertySet>>> streamItems;

    @InjectMocks
    private SoundStreamFragment fragment;

    @Mock
    private SoundStreamOperations soundStreamOperations;
    @Mock
    private SoundStreamFragment.StreamItemAdapter adapter;
    @Mock
    private ListViewController listViewController;
    @Mock
    private PullToRefreshController pullToRefreshController;
    @Mock
    private EventBus eventBus;
    @Mock
    private Subscription subscription;

    @Before
    public void setup() {
        streamItems = TestObservables.emptyConnectableObservable(subscription);
        when(soundStreamOperations.existingStreamItems()).thenReturn(streamItems);
        when(soundStreamOperations.updatedStreamItems()).thenReturn(streamItems);
    }

    @Test
    public void shouldRequestAvailableSoundStreamItemsWhenCreated() {
        createFragment();
        verify(soundStreamOperations).existingStreamItems();
        verify(adapter).onCompleted();
    }

    @Test
    public void shouldAttachListViewControllerInOnViewCreated() {
        fragment.connectObservable(streamItems);
        createFragmentView();
        verify(listViewController).onViewCreated(refEq(fragment), refEq(streamItems),
                refEq(fragment.getView()), refEq(adapter), refEq(adapter));
    }

    @Test
    public void shouldAttachPullToRefreshControllerInOnViewCreated() {
        fragment.onCreate(null);
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

    private void createFragment() {
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