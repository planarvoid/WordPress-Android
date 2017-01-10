package com.soundcloud.android.sync.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Collections;

public class PublishPlaylistUpdateEventCommandTest extends AndroidUnitTest {

    @Mock private EventBus eventBus;
    @Captor private ArgumentCaptor<PlaylistEntityChangedEvent> playlistEntityChangedEventArgumentCaptor;

    private PublishPlaylistUpdateEventCommand publishPlaylistUpdateEventCommand;

    @Before
    public void setUp() throws Exception {
        publishPlaylistUpdateEventCommand = new PublishPlaylistUpdateEventCommand(eventBus);
    }

    @Test
    public void sendsConvertedPlaylistItem() throws Exception {
        final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

        publishPlaylistUpdateEventCommand.call(Collections.singletonList(apiPlaylist));

        verify(eventBus).publish(eq(EventQueue.PLAYLIST_CHANGED), playlistEntityChangedEventArgumentCaptor.capture());
        final PlaylistEntityChangedEvent changedEvent = playlistEntityChangedEventArgumentCaptor.getValue();
        assertThat(changedEvent.kind()).isEqualTo(PlaylistChangedEvent.Kind.PLAYLIST_UPDATED);
        assertThat(changedEvent.changeMap().values()).containsExactly(PlaylistItem.from(apiPlaylist));
    }

    @Test
    public void doesNotSendEmptyEvent() throws Exception {
        publishPlaylistUpdateEventCommand.call(Collections.emptyList());

        verify(eventBus, never()).publish(eq(EventQueue.PLAYLIST_CHANGED), any(PlaylistChangedEvent.class));
    }
}
