package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.DialogInterface;

public class OfflineLikesDialogTest extends AndroidUnitTest {

    public static final String PAGE_NAME = "page_name";
    @Mock OfflineContentOperations operations;
    @Mock ScreenProvider screenProvider;

    private TestEventBus eventBus;
    private OfflineLikesDialog dialog;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        dialog = new OfflineLikesDialog(operations, screenProvider, eventBus);
    }

    @Test
    public void sendsTrackingEventWhenAddingOfflineLikes() {
        when(operations.enableOfflineLikedTracks()).thenReturn(Observable.empty());
        when(screenProvider.getLastScreenTag()).thenReturn(PAGE_NAME);
        dialog.onClick(mock(DialogInterface.class), 0);

        OfflineInteractionEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING,
                                                                     OfflineInteractionEvent.class);
        assertThat(trackingEvent.offlineContentContext().get()).isEqualTo(OfflineInteractionEvent.OfflineContentContext.LIKES_CONTEXT);
        assertThat(trackingEvent.isEnabled().get()).isEqualTo(true);
        assertThat(trackingEvent.clickName().get()).isEqualTo(OfflineInteractionEvent.Kind.KIND_OFFLINE_LIKES_ADD);
        assertThat(trackingEvent.pageName().get()).isEqualTo(PAGE_NAME);
    }

}
