package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;

import android.content.DialogInterface;

public class OfflineLikesDialogTest extends AndroidUnitTest {

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
        when(operations.enableOfflineLikedTracks()).thenReturn(Observable.<Boolean>empty());
        when(screenProvider.getLastScreenTag()).thenReturn("page_name");
        dialog.onClick(mock(DialogInterface.class), 0);

        TrackingEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.getKind()).isEqualTo(UIEvent.KIND_OFFLINE_LIKES_ADD);
        assertThat(trackingEvent.getAttributes()
                .containsValue("page_name")).isTrue();
    }

}
