package com.soundcloud.android.sync.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Collections;

public class LegacyPublishEntityUpdateEventTest extends AndroidUnitTest {

    @Mock private EventBus eventBus;
    @Captor private ArgumentCaptor<EntityStateChangedEvent> entityStateChangedEventArgumentCaptor;

    private LegacyPublishEntityUpdateEvent publishEntityUpdateEvent;

    @Before
    public void setUp() throws Exception {
        publishEntityUpdateEvent = new LegacyPublishEntityUpdateEvent(eventBus);
    }

    @Test
    public void sendsConvertedPlaylistItem() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);

        publishEntityUpdateEvent.call(Collections.singletonList(apiTrack));

        verify(eventBus).publish(eq(EventQueue.ENTITY_STATE_CHANGED), entityStateChangedEventArgumentCaptor.capture());
        final EntityStateChangedEvent changedEvent = entityStateChangedEventArgumentCaptor.getValue();
        assertThat(changedEvent.getKind()).isEqualTo(EntityStateChangedEvent.UPDATED);
        assertThat(changedEvent).isEqualTo(TrackItem.from(apiTrack).toUpdateEvent());
    }

    @Test
    public void doesNotSendEmptyEvent() throws Exception {
        publishEntityUpdateEvent.call(Collections.emptyList());

        verify(eventBus, never()).publish(eq(EventQueue.ENTITY_STATE_CHANGED), any(EntityStateChangedEvent.class));
    }
}
