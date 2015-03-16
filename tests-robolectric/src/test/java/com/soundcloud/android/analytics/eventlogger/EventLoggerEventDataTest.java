package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerEventDataTest {

    @Test
    public void implementsEqualsContract() throws Exception {
        EqualsVerifier.forClass(EventLoggerEventData.class)
                .suppress(Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void serializesFullEvent() throws Exception {
        final EventLoggerEventData source = new EventLoggerEventData("event", "v1", "1234", "4321", "user:1", "999").pageName("page");
        expect(new JacksonJsonTransformer().toJson(source)).toEqual("{\"event\":\"event\",\"version\":\"v1\",\"payload\":{\"anonymous_id\":\"4321\",\"page_name\":\"page\",\"user\":\"user:1\",\"client_id\":\"1234\",\"ts\":\"999\"}}");

    }
}