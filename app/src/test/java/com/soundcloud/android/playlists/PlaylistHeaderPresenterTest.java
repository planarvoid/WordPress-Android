package com.soundcloud.android.playlists;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.v4.app.Fragment;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistHeaderPresenterTest {
    @Mock
    private PlaylistViewHeaderPresenter playlistViewHeaderPresenter;
    @Mock
    private Fragment fragment;
    @Mock
    private PlaylistDetailsViewFactory playlistDetailsViewFactory;

    private PlaylistHeaderPresenter presenter;
    private TestEventBus eventBus;

    @Before
    public void setup() {
        eventBus = new TestEventBus();
        presenter = new PlaylistHeaderPresenter(eventBus, playlistDetailsViewFactory, playlistViewHeaderPresenter);
    }

    @Test
    public void updatesViewHeaderPresenterOnEntityStateChange() {
        presenter.onResume(fragment);
        EntityStateChangedEvent event = mock(EntityStateChangedEvent.class);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);

        verify(playlistViewHeaderPresenter).update(event);
    }

    @Test
    public void unsubscribesFromOngoingSubscriptionsWhenActivityDestroyed() {
        presenter.onResume(fragment);

        presenter.onPause(fragment);

        eventBus.verifyUnsubscribed();
    }
}