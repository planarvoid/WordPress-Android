package com.soundcloud.android.sync.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackChangedEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Collections;

public class PublishTrackUpdateEventCommandTest extends AndroidUnitTest {

    @Mock private EventBus eventBus;
    @Captor private ArgumentCaptor<TrackChangedEvent> trackChangedEventArgumentCaptor;

    private PublishTrackUpdateEventCommand publishTrackUpdateEventCommand;

    @Before
    public void setUp() throws Exception {
        publishTrackUpdateEventCommand = new PublishTrackUpdateEventCommand(eventBus);
    }

    @Test
    public void sendsConvertedPlaylistItem() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);

        publishTrackUpdateEventCommand.call(Collections.singletonList(apiTrack));

        verify(eventBus).publish(eq(EventQueue.TRACK_CHANGED), trackChangedEventArgumentCaptor.capture());
        final TrackChangedEvent changedEvent = trackChangedEventArgumentCaptor.getValue();
        assertThat(changedEvent.changeMap().values()).containsExactly(Track.from(apiTrack));
    }

    @Test
    public void doesNotSendEmptyEvent() throws Exception {
        publishTrackUpdateEventCommand.call(Collections.emptyList());

        verify(eventBus, never()).publish(eq(EventQueue.TRACK_CHANGED), any(TrackChangedEvent.class));
    }
}
