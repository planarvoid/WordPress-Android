package com.soundcloud.android.analytics.eventlogger;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

public class EventLoggerEventDataV1Test extends AndroidUnitTest {

    private static final int CLIENT_ID = 12;

    @Test
    public void implementsEqualsContract() throws Exception {
        EqualsVerifier.forClass(EventLoggerEventData.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void addsClientIdAsInt() {
        EventLoggerEventData data = new EventLoggerEventDataV1("event", "v0", CLIENT_ID, "1234", "4321", 12345L, "conn-type");

        assertThat(data.payload.get(EventLoggerParam.CLIENT_ID)).isEqualTo(CLIENT_ID);
    }

    @Test
    public void addsTimestampAsLong() {
        EventLoggerEventData data = new EventLoggerEventDataV1("event", "v0", CLIENT_ID, "1234", "4321", 12345L, "conn-type");

        assertThat(data.payload.get(EventLoggerParam.TIMESTAMP)).isEqualTo(12345L);
    }

    @Test
    public void addsLocalStoragePlaybackAsBoolean() {
        EventLoggerEventData data = new EventLoggerEventDataV1("event", "v0", CLIENT_ID, "1234", "4321", 12345L, "conn-type");

        data.localStoragePlayback(true);

        assertThat(data.payload.get(EventLoggerParam.LOCAL_STORAGE_PLAYBACK)).isEqualTo(true);
    }

}
