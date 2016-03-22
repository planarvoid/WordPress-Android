package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.utils.BuildHelper;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class ForceUpdateHandlerTest {

    private TestEventBus eventBus = new TestEventBus();
    private ForceUpdateHandler handler;

    @Mock private BuildHelper buildHelper;
    @Mock private DeviceHelper deviceHelper;
    @Mock private ConfigurationSettingsStorage storage;

    @Before
    public void setUp() throws Exception {
        handler = new ForceUpdateHandler(eventBus, buildHelper, deviceHelper, storage);
    }

    @Test
    public void shouldNotPublishForceUpdateEventWhenSelfDestructFlagAbsent() throws IOException {
        handler.checkForForcedUpdate(normalConfiguration());

        assertThat(eventBus.eventsOn(EventQueue.FORCE_UPDATE)).isEmpty();
    }

    @Test
    public void shouldPublishForceUpdateEventWhenSelfDestructFlagPresent() throws IOException {
        handler.checkForForcedUpdate(forceUpdateConfiguration());

        assertThat(eventBus.eventsOn(EventQueue.FORCE_UPDATE)).hasSize(1);
    }

    @Test
    public void shouldPersistBlacklistedVersionWhenSelfDestructFlagPresent() {
        when(deviceHelper.getAppVersionCode()).thenReturn(123);

        handler.checkForForcedUpdate(forceUpdateConfiguration());

        verify(storage).storeForceUpdateVersion(123);
    }

    @Test
    public void shouldClearBlacklistedVersionWhenSelfDestructFlagAbsent() {
        handler.checkForForcedUpdate(normalConfiguration());

        verify(storage).clearForceUpdateVersion();
    }

    @Test
    public void checkPendingUpdatesShouldPublishUpdateEventIfVersionsMatch() {
        when(storage.getForceUpdateVersion()).thenReturn(123);
        when(deviceHelper.getAppVersionCode()).thenReturn(123);

        handler.checkPendingForcedUpdate();

        assertThat(eventBus.eventsOn(EventQueue.FORCE_UPDATE)).hasSize(1);
    }

    @Test
    public void checkPendingUpdatesShouldNotPublishUpdateEventIfVersionsDontMatch() {
        when(storage.getForceUpdateVersion()).thenReturn(123);
        when(deviceHelper.getAppVersionCode()).thenReturn(456);

        handler.checkPendingForcedUpdate();

        assertThat(eventBus.eventsOn(EventQueue.FORCE_UPDATE)).isEmpty();
    }

    private Configuration normalConfiguration() {
        return Configuration.builder()
                .selfDestruct(false)
                .build();
    }

    private Configuration forceUpdateConfiguration() {
        return Configuration.builder()
                .selfDestruct(true)
                .build();
    }
}
