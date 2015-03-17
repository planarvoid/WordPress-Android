package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import dagger.Lazy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerDataBuilderFactoryTest {

    @Mock Lazy<EventLoggerJsonDataBuilder> jsonDataBuilder;
    @Mock Lazy<EventLoggerUrlDataBuilder> urlDataBuilder;

    private EventLoggerDataBuilderFactory factory;

    @Before
    public void setUp() throws Exception {
        when(jsonDataBuilder.get()).thenReturn(mock(EventLoggerJsonDataBuilder.class));
        when(urlDataBuilder.get()).thenReturn(mock(EventLoggerUrlDataBuilder.class));
        factory = new EventLoggerDataBuilderFactory(jsonDataBuilder, urlDataBuilder);
    }

    @Test
    public void createsJsonBuilderForBoogaloo() {
        EventLoggerDataBuilder dataBuilder = factory.create(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        expect(dataBuilder).toBeInstanceOf(EventLoggerJsonDataBuilder.class);
    }

    @Test
    public void createsUrlBuilderForLegacyBackend() {
        EventLoggerDataBuilder dataBuilder = factory.create(EventLoggerAnalyticsProvider.LEGACY_BACKEND_NAME);
        expect(dataBuilder).toBeInstanceOf(EventLoggerUrlDataBuilder.class);
    }

}
