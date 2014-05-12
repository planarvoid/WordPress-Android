package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.TestObservables.MockObservable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.FrameLayout;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamFragmentTest {

    private SoundStreamFragment fragment;
    private FragmentActivity activity = new FragmentActivity();
    private MockObservable streamItems = TestObservables.emptyObservable();

    @Mock
    private SoundStreamOperations soundStreamOperations;
    @Mock
    private SoundStreamFragment.StreamItemAdapter adapter;
    @Mock
    private PullToRefreshController pullToRefreshController;
    @Mock
    private EventBus eventBus;

    @Before
    public void setup() {
        fragment = new SoundStreamFragment(soundStreamOperations, adapter, pullToRefreshController, eventBus);
        when(soundStreamOperations.existingStreamItems()).thenReturn(streamItems);
    }

    @Test
    public void shouldRequestAvailableSoundStreamItemsWhenCreated() {
        fragment.onViewCreated(createFragmentViews(), null);
        expect(streamItems.subscribedTo()).toBeTrue();
    }

    @Test
    public void shouldSetupPullToRefreshController() {
        fragment.onViewCreated(createFragmentViews(), null);
        verify(pullToRefreshController).attach(activity, (PullToRefreshLayout) fragment.getView(), fragment);
    }

    @Test
    public void onRefreshShouldRequestSoundStreamToRefresh() {
        MockObservable refreshedItems = TestObservables.emptyObservable();
        when(soundStreamOperations.updatedStreamItems()).thenReturn(refreshedItems);
        fragment.onViewCreated(createFragmentViews(), null);

        fragment.onRefreshStarted(fragment.getView());

        expect(refreshedItems.subscribedTo()).toBeTrue();
    }

    @Test
    public void shouldHidePullToRefreshProgressWhenRefreshSucceeds() {
        MockObservable refreshedItems = TestObservables.emptyObservable();
        when(soundStreamOperations.updatedStreamItems()).thenReturn(refreshedItems);
        fragment.onViewCreated(createFragmentViews(), null);

        fragment.onRefreshStarted(fragment.getView());

        verify(pullToRefreshController).stopRefreshing();
    }

    @Test
    public void shouldHidePullToRefreshProgressWhenRefreshFails() {
        MockObservable refreshedItems = TestObservables.errorObservable();
        when(soundStreamOperations.updatedStreamItems()).thenReturn(refreshedItems);
        fragment.onViewCreated(createFragmentViews(), null);

        fragment.onRefreshStarted(fragment.getView());

        verify(pullToRefreshController).stopRefreshing();
    }

    private View createFragmentViews() {
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);
        fragment.onCreate(null);
        View view = fragment.onCreateView(activity.getLayoutInflater(), new FrameLayout(activity), null);
        Robolectric.shadowOf(fragment).setView(view);
        return view;
    }
}