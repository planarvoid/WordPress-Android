package com.soundcloud.android.analytics.eventlogger;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

public class EventLoggerEventDataTest extends AndroidUnitTest {

    private static final int CLIENT_ID = 12;

    @Test
    public void implementsEqualsContract() throws Exception {
        EqualsVerifier.forClass(EventLoggerEventData.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void addsClientIdAsString() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        assertThat(data.payload.get(EventLoggerParam.CLIENT_ID)).isEqualTo(String.valueOf(CLIENT_ID));
    }

    @Test
    public void addsTimestampAsString() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        assertThat(data.payload.get(EventLoggerParam.TIMESTAMP)).isEqualTo(String.valueOf(12345));
    }

    @Test
    public void addsLocalStoragePlaybackAsString() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        data.localStoragePlayback(true);

        assertThat(data.payload.get(EventLoggerParam.LOCAL_STORAGE_PLAYBACK)).isEqualTo(String.valueOf(true));
    }

    @Test
    public void addsParamWithValueToPayload() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        data.clickName("clicky");

        assertThat(data.payload.get(EventLoggerParam.CLICK_NAME)).isEqualTo("clicky");
    }

    @Test
    public void excludesEmptyParamsFromPayload() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        data.adUrn(null);
        data.clickObject("");

        assertThat(data.payload.containsKey(EventLoggerParam.AD_URN)).isFalse();
        assertThat(data.payload.containsKey(EventLoggerParam.CLICK_OBJECT)).isFalse();
    }

}
