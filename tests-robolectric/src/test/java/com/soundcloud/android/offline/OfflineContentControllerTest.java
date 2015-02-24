package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.doReturn;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentControllerTest {

    @Mock
    private OfflineContentOperations operations;

    private OfflineContentController controller;

    private PublishSubject<Object> startSyncer;
    private PublishSubject<Object> stopSyncer;

    @Before
    public void setUp() throws Exception {
        startSyncer = PublishSubject.create();
        stopSyncer = PublishSubject.create();
        doReturn(startSyncer).when(operations).startOfflineContent();
        doReturn(stopSyncer).when(operations).stopOfflineContentService();
        controller = new OfflineContentController(operations, Robolectric.application);
    }

    @Test
    public void startsOfflineContentService() {
        controller.subscribe();

        startSyncer.onNext(new Object());

        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService.getAction()).toEqual(OfflineContentService.ACTION_START_DOWNLOAD);
        expect(startService.getComponent().getClassName()).toEqual(OfflineContentService.class.getCanonicalName());
    }

    @Test
    public void stopsOfflineContentService() {
        controller.subscribe();

        stopSyncer.onNext(new Object());

        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService.getAction()).toEqual(OfflineContentService.ACTION_STOP_DOWNLOAD);
        expect(startService.getComponent().getClassName()).toEqual(OfflineContentService.class.getCanonicalName());
    }

    @Test
    public void ignoreStartEventsWhenUnsubscribed() {
        controller.subscribe();
        controller.unsubscribe();

        startSyncer.onNext(new Object());

        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService).toBeNull();
    }


    @Test
    public void ignoreStopEventsWhenUnsubscribed() {
        controller.subscribe();
        controller.unsubscribe();

        stopSyncer.onNext(new Object());

        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService).toBeNull();
    }
}
