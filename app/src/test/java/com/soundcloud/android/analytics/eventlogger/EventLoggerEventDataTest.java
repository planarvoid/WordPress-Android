package com.soundcloud.android.analytics.eventlogger;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.PlatformUnitTest;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

public class EventLoggerEventDataTest extends PlatformUnitTest {

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

        assertThat(data.payload.get(EventLoggerParam.CLICK_NAME)).isEqualTo("clicky");
    }

    @Test
    public void excludesEmptyParamsFromPayload() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", "client", "1234", "4321", "123");

        data.adUrn(null);
        data.clickObject("");

        assertThat(data.payload.containsKey(EventLoggerParam.AD_URN)).isFalse();
        assertThat(data.payload.containsKey(EventLoggerParam.CLICK_OBJECT)).isFalse();
    }

}
