package com.soundcloud.android.actionbar;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class PullToRefreshControllerTest {

    @Mock
    private EventBus eventBus;
    @Mock
    private FragmentActivity activity;
    @Mock
    private OnRefreshListener listener;
    @Mock
    private PullToRefreshAttacher attacher;
    @Mock
    private PullToRefreshLayout layout;

    private PullToRefreshController controller;

    @Before
    public void setUp() throws Exception {
        controller = new PullToRefreshController(eventBus, attacher);
        when(eventBus.subscribe(any(EventBus.QueueDescriptor.class), any(Subscriber.class))).thenReturn(Subscriptions.empty());
    }

    @Test
    public void shouldAttachPullToRefresh() {
        expect(controller.isAttached()).toBeFalse();
        controller.attach(activity, layout, listener);

        verify(attacher).attach(activity, layout, listener);
        expect(controller.isAttached()).toBeTrue();
    }

    @Test
    public void shouldSubscribeToPlayerUIEventQueueOnAttach() {
        EventMonitor monitor = EventMonitor.on(eventBus);

        controller.attach(activity, layout, listener);

        monitor.verifySubscribedTo(EventQueue.PLAYER_UI);
    }

    @Test
    public void shouldUnsubscribeOnDetach() {
        EventMonitor monitor = EventMonitor.on(eventBus);
        controller.attach(activity, layout, listener);

        controller.detach();

        monitor.verifyUnsubscribed();
    }

    @Test
    public void shouldDetachFromActivityOnDetach() {
        controller.attach(activity, layout, listener);

        controller.detach();
        expect(controller.isAttached()).toBeFalse();
    }

    @Ignore // Final method. Cannot mock. :(
    @Test
    public void shouldStopRefreshingWhenPlayerExpandedEventIsReceived() {
        EventMonitor monitor = EventMonitor.on(eventBus);
        controller.attach(activity, layout, listener);

        monitor.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(layout).setRefreshComplete();
    }

}