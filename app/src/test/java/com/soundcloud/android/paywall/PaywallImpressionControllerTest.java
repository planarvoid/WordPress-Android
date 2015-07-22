package com.soundcloud.android.paywall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.MidTierTrackEvent;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class PaywallImpressionControllerTest extends AndroidUnitTest {

    private static final int ITEM_POSITION = 1;
    private static final String PAGE_NAME = "page_name";
    private PaywallImpressionController impressionCreator;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private RecyclerView recyclerView;
    @Mock private LinearLayoutManager linearLayoutManager;
    @Mock private View itemView;
    @Mock private Handler handler;
    @Mock private ScreenProvider screenProvider;
    @Mock(extraInterfaces = ItemAdapter.class) private RecyclerView.Adapter itemAdapter;
    @Captor ArgumentCaptor<RecyclerView.OnChildAttachStateChangeListener> childAttachCaptor;

    @Before
    public void setUp() throws Exception {
        when(screenProvider.getLastScreenTag()).thenReturn(PAGE_NAME);
        impressionCreator = new PaywallImpressionController(eventBus, handler, screenProvider);
        when(recyclerView.getLayoutManager()).thenReturn(linearLayoutManager);
        when(recyclerView.getAdapter()).thenReturn(itemAdapter);
        when(itemAdapter.getItemCount()).thenReturn(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onAttachThrowsIllegalArgumentExceptionWithNoAdapter() {
        when(recyclerView.getAdapter()).thenReturn(null);
        impressionCreator.attachRecyclerView(recyclerView);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onAttachThrowsIllegalArgumentExceptionWithWrongTypeOfAdapter() {
        when(recyclerView.getAdapter()).thenReturn(mock(RecyclerView.Adapter.class));
        impressionCreator.attachRecyclerView(recyclerView);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onAttachThrowsIllegalArgumentExceptionWithNoLayoutManager() {
        when(recyclerView.getLayoutManager()).thenReturn(null);
        impressionCreator.attachRecyclerView(recyclerView);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onAttachThrowsIllegalArgumentExceptionWithWrongTypeOfLayoutManager() {
        when(recyclerView.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
        impressionCreator.attachRecyclerView(recyclerView);
    }

    @Test
    public void onChildViewAttachedDoesNotFiresImpressionEventForNonMidTierTrack() {
        attachItemView(TrackItem.from(TestPropertySets.fromApiTrack()));

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void onChildViewAttachedDoesNotFiresImpressionEventForPlaylistItem() {
        attachItemView(ModelFixtures.create(PlaylistItem.class));

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void onChildViewAttachedDoesNotFiresImpressionEventForUserItem() {
        attachItemView(ModelFixtures.create(UserItem.class));

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void onChildViewAttachedFiresImpressionEventForMidTierTrackIfNotDeduplicating() {
        final PropertySet midTierTrack = TestPropertySets.midTierTrack();
        attachItemView(TrackItem.from(midTierTrack));

        final MidTierTrackEvent event1 = (MidTierTrackEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event1.getTrackUrn()).isEqualTo(midTierTrack.get(TrackProperty.URN));
        assertThat(event1.getPageName()).isEqualTo(PAGE_NAME);
    }

    @Test
    public void onChildViewAttachedDoesNotFireImpressionEventForMidTierTrackIfDeduplicating() {
        final PropertySet midTierTrack = TestPropertySets.midTierTrack();
        when(handler.hasMessages(PaywallImpressionController.HANDLER_MESSAGE, midTierTrack.get(TrackProperty.URN))).thenReturn(true);

        attachItemView(TrackItem.from(midTierTrack));

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void onChildViewDetachedSendsItemToDeduplicationHandler() {
        final PropertySet midTierTrack = TestPropertySets.midTierTrack();

        detachItemView(TrackItem.from(midTierTrack));

        final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(handler).sendMessage(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getValue().obj).isEqualTo(midTierTrack.get(TrackProperty.URN));
    }

    @Test
    public void detachRecyclerViewDetatchesViewListener() {
        impressionCreator.attachRecyclerView(recyclerView);
        verify(recyclerView).addOnChildAttachStateChangeListener(childAttachCaptor.capture());

        impressionCreator.detachRecyclerView(recyclerView);
        verify(recyclerView).removeOnChildAttachStateChangeListener(childAttachCaptor.getValue());
    }

    @Test //fixes: https://github.com/soundcloud/SoundCloud-Android/issues/3413
    public void onChildViewDetachedFromWindowDoesNotCrashWhenItemNotFound() {
        when(itemAdapter.getItemCount()).thenReturn(1); // track was removed by unliking
        final PropertySet midTierTrack = TestPropertySets.midTierTrack();

        detachItemView(TrackItem.from(midTierTrack));

        verify(itemAdapter, never()).getItemId(ITEM_POSITION);
    }

    private void attachItemView(ListItem listItem) {
        setupItemPosition(listItem);

        verify(recyclerView).addOnChildAttachStateChangeListener(childAttachCaptor.capture());
        childAttachCaptor.getValue().onChildViewAttachedToWindow(itemView);
    }

    private void detachItemView(ListItem listItem) {
        setupItemPosition(listItem);

        verify(recyclerView).addOnChildAttachStateChangeListener(childAttachCaptor.capture());
        childAttachCaptor.getValue().onChildViewDetachedFromWindow(itemView);
    }

    private void setupItemPosition(ListItem listItem) {
        when(((ItemAdapter) itemAdapter).getItem(ITEM_POSITION)).thenReturn(listItem);
        when(linearLayoutManager.getPosition(itemView)).thenReturn(ITEM_POSITION);
        impressionCreator.attachRecyclerView(recyclerView);
    }
}