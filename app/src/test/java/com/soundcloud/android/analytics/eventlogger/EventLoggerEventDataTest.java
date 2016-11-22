package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.analytics.eventlogger.EventLoggerEventData.ITEM_INTERACTION;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.assertj.core.util.Maps;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

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

    @Test
    public void addsExperimentToPayload() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        data.experiment("exp_android_listening", 12345);

        assertThat(data.payload.get("exp_android_listening")).isEqualTo(String.valueOf(12345));
    }

    @Test
    public void addsPageviewIdToPayload() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        data.pageviewId("uuid");

        assertThat(data.payload.get(EventLoggerParam.PAGEVIEW_ID)).isEqualTo("uuid");
    }

    @Test
    public void addsAttributingActivityToPayload() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        data.attributingActivity("some_activity_type", "resource");

        Map<String, String> expected = Maps.newHashMap();
        expected.put(EventLoggerParam.ACTIVITY_TYPE, "some_activity_type");
        expected.put(EventLoggerParam.RESOURCE, "resource");
        assertThat(data.payload.get(EventLoggerParam.ATTRIBUTING_ACTIVITY)).isEqualTo(expected);
    }

    @Test
    public void addsLinkTypeToPayload() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        data.linkType("link_type");

        assertThat(data.payload.get(EventLoggerParam.LINK_TYPE)).isEqualTo("link_type");
    }

    @Test
    public void addsItemToPayload() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        data.item("item");

        assertThat(data.payload.get(EventLoggerParam.ITEM)).isEqualTo("item");
    }

    @Test
    public void addsModuleToPayloadAsContext() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        data.module("module_name");

        Map<String, String> expected = Maps.newHashMap();
        expected.put(EventLoggerParam.MODULE_NAME, "module_name");
        assertThat(data.payload.get(EventLoggerParam.MODULE)).isEqualTo(expected);
    }

    @Test
    public void addsModulePositionToPayloadAsPositionInContext() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        data.modulePosition(0);

        assertThat(data.payload.get(EventLoggerParam.MODULE_POSITION)).isEqualTo("0");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldMapScreenEventKindToReferringEventName() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        final String uuid = UUID.randomUUID().toString();

        data.referringEvent(uuid, ScreenEvent.KIND);

        final Map<String, String> actual = (Map<String, String>) data.payload.get(EventLoggerParam.REFERRING_EVENT);

        assertThat(actual.get(EventLoggerParam.UUID)).isEqualTo(uuid);
        assertThat(actual.get(EventLoggerParam.REFERRING_EVENT_KIND)).isEqualTo("pageview");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldMapForegroundEventKindToReferringEventName() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        final String uuid = UUID.randomUUID().toString();

        data.referringEvent(uuid, ForegroundEvent.KIND_OPEN);

        final Map<String, String> actual = (Map<String, String>) data.payload.get(EventLoggerParam.REFERRING_EVENT);

        assertThat(actual.get(EventLoggerParam.UUID)).isEqualTo(uuid);
        assertThat(actual.get(EventLoggerParam.REFERRING_EVENT_KIND)).isEqualTo("foreground");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldMapNavigationEventKindToReferringEventName() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        final String uuid = UUID.randomUUID().toString();

        data.referringEvent(uuid, UIEvent.KIND_NAVIGATION);

        final Map<String, String> actual = (Map<String, String>) data.payload.get(EventLoggerParam.REFERRING_EVENT);

        assertThat(actual.get(EventLoggerParam.UUID)).isEqualTo(uuid);
        assertThat(actual.get(EventLoggerParam.REFERRING_EVENT_KIND)).isEqualTo(ITEM_INTERACTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUnknownReferringEventKind() {
        EventLoggerEventData data = new EventLoggerEventData("event", "v0", CLIENT_ID, "1234", "4321", 12345);

        final String uuid = UUID.randomUUID().toString();

        data.referringEvent(uuid, "Random Event Type");
    }
}
