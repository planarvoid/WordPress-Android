package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.*;

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
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void addsParamWithValueToPayload() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", "client", "1234", "4321", "123");

        data.clickName("clicky");

        expect(data.payload.get(CLICK_NAME)).toEqual("clicky");
    }

    @Test
    public void excludesEmptyParamsFromPayload() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", "client", "1234", "4321", "123");

        data.adUrn(null);
        data.clickObject("");

        expect(data.payload.containsKey(AD_URN)).toBeFalse();
        expect(data.payload.containsKey(CLICK_OBJECT)).toBeFalse();
    }

}
