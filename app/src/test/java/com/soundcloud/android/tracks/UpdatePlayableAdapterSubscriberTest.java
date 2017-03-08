package com.soundcloud.android.tracks;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.adapters.PlayableViewItem;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

public class UpdatePlayableAdapterSubscriberTest extends AndroidUnitTest {

    @Mock private RecyclerItemAdapter adapter;
    @Mock private PlayableViewItem<PlayableViewItem> playableViewItem;
    @Mock private Object differentItem;
    @Mock private CellRenderer<DiscoveryItem> cellRenderer;
    @Mock private CurrentPlayQueueItemEvent eventWithTrackAndCollection;
    @Mock private PlayableViewItem<CurrentPlayQueueItemEvent> updatedPlayableViewItem;

    private UpdatePlayableAdapterSubscriber subscriber;
    private List discoveryItems;

    @Before
    public void setUp() throws Exception {
        discoveryItems = Lists.newArrayList(playableViewItem, differentItem);
        when(adapter.getItems()).thenReturn(discoveryItems);
        subscriber = new UpdatePlayableAdapterSubscriber(adapter);
    }

    @Test
    public void updatesNowPlayingOfPlayableItemAndNotifiesItemWhenUpdated() {
        when(playableViewItem.updateNowPlaying(eventWithTrackAndCollection)).thenReturn(updatedPlayableViewItem);

        subscriber.onNext(eventWithTrackAndCollection);

        verify(playableViewItem).updateNowPlaying(eventWithTrackAndCollection);
        verifyZeroInteractions(differentItem);
        verify(adapter).notifyItemChanged(discoveryItems.indexOf(updatedPlayableViewItem));
    }

    @Test
    public void updatesNowPlayingOfPlayableItemAndDoesNotNotifyItemWhenNotUpdated() {
        when(playableViewItem.updateNowPlaying(eventWithTrackAndCollection)).thenReturn(playableViewItem);

        subscriber.onNext(eventWithTrackAndCollection);

        verify(playableViewItem).updateNowPlaying(eventWithTrackAndCollection);
        verifyZeroInteractions(differentItem);
        verify(adapter, never()).notifyItemChanged(anyInt());
    }
}
