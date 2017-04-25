package com.soundcloud.android.mrlocallocal;

import static com.soundcloud.android.mrlocallocal.SpecValidator.KEY_TIMESTAMP;
import static com.soundcloud.android.mrlocallocal.SpecValidator.KEY_WHITELISTED_ALL;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.mrlocallocal.data.LoggedEvent;
import com.soundcloud.android.mrlocallocal.data.MrLocalLocalException;
import com.soundcloud.android.mrlocallocal.data.Spec;
import com.soundcloud.android.mrlocallocal.data.SpecEvent;
import com.soundcloud.java.collections.Pair;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class SpecValidatorTest {

    private SpecValidator specValidator;

    @Before
    public void setUp() throws Exception {
        specValidator = new SpecValidator(new TestLogger());
    }

    @Test
    public void emptySpecAndEmptyLog() throws Exception {
        List<String> whiteListedEvents = Lists.emptyList();
        List<SpecEvent> expectedEvents = Lists.emptyList();
        List<LoggedEvent> events = Lists.emptyList();

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isTrue();
    }

    @Test
    public void sortsLoggedEventsByTimestamp() throws Exception {
        Event event1 = Event.event("audio", tsParam(1));
        Event event2 = Event.event("audio", tsParam(2));

        List<String> whiteListedEvents = Lists.emptyList();
        List<SpecEvent> expectedEvents = Arrays.asList(event1.toSpecEvent(), event2.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(event2.toLoggedEvent(), event1.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isTrue();
    }

    @Test
    public void ignoresNotWhitelistedEvents() throws Exception {
        Event audio = Event.event("audio", tsParam(1));
        Event pageview = Event.event("pageview", tsParam(2));
        Event click = Event.event("click", tsParam(3));

        List<String> whiteListedEvents = Arrays.asList("audio");
        List<SpecEvent> expectedEvents = Arrays.asList(audio.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(audio.toLoggedEvent(), pageview.toLoggedEvent(), click.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isTrue();
    }

    @Test
    public void validatesAllWhitelistedEvents() throws Exception {
        Event audio = Event.event("audio", tsParam(1));
        Event pageview = Event.event("pageview", tsParam(2));
        Event click = Event.event("click", tsParam(3));

        List<String> whiteListedEvents = Arrays.asList("audio", "pageview");
        List<SpecEvent> expectedEvents = Arrays.asList(audio.toSpecEvent(), pageview.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(audio.toLoggedEvent(), pageview.toLoggedEvent(), click.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isTrue();
    }

    @Test
    public void validatesAllEventsWhenWhitelisted_all() throws Exception {
        Event audio = Event.event("audio", tsParam(1));
        Event pageview = Event.event("pageview", tsParam(2));
        Event click = Event.event("click", tsParam(3));

        List<String> whiteListedEvents = Arrays.asList(KEY_WHITELISTED_ALL);
        List<SpecEvent> expectedEvents = Arrays.asList(audio.toSpecEvent(), pageview.toSpecEvent(), click.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(audio.toLoggedEvent(), pageview.toLoggedEvent(), click.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isTrue();
    }

    @Test
    public void validatesAllEventsWhenNoWhitelistSet() throws Exception {
        Event audio = Event.event("audio", tsParam(1));
        Event pageview = Event.event("pageview", tsParam(2));
        Event click = Event.event("click", tsParam(3));

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(audio.toSpecEvent(), pageview.toSpecEvent(), click.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(audio.toLoggedEvent(), pageview.toLoggedEvent(), click.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isTrue();
    }

    @Test
    public void validatesOptionalEventThatsPresent() throws Exception {
        Event audio = Event.event("audio", tsParam(1));
        Event optionalAudio = Event.optionalEvent("audio", tsParam(2));

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(audio.toSpecEvent(), optionalAudio.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(optionalAudio.toLoggedEvent(), audio.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isTrue();
    }

    @Test
    public void validatesOptionalEventThatsAbsent() throws Exception {
        Event audio = Event.event("audio", tsParam(1));
        Event optionalAudio = Event.optionalEvent("audio", tsParam(2));

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(audio.toSpecEvent(), optionalAudio.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(audio.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isTrue();
    }

    @Test
    public void failsForTooManyEvents() throws Exception {
        Event audio = Event.event("audio", tsParam(1));

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList();
        List<LoggedEvent> events = Arrays.asList(audio.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isFalse();
    }

    @Test
    public void failsForTooFewEvents() throws Exception {
        Event audio = Event.event("audio", tsParam(1));

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(audio.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList();

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isFalse();
    }

    @Test
    public void failsforEventsInWrongOrder() throws Exception {
        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                Event.event("audio", fieldParam(1, Pair.of("test", "first"))).toSpecEvent(),
                Event.event("audio", fieldParam(2, Pair.of("test", "second"))).toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                Event.event("audio", fieldParam(1, Pair.of("test", "second"))).toLoggedEvent(),
                Event.event("audio", fieldParam(2, Pair.of("test", "first"))).toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isFalse();
    }

    @Test
    public void failsWhenMultipleEventsDontMatch() throws Exception {
        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                Event.event("audio", fieldParam(1, Pair.of("test", "first"))).toSpecEvent(),
                Event.event("audio", fieldParam(2, Pair.of("test", "second"))).toSpecEvent(),
                Event.event("audio", fieldParam(3, Pair.of("test", "third"))).toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                Event.event("audio", fieldParam(1, Pair.of("test", "second"))).toLoggedEvent(),
                Event.event("audio", fieldParam(2, Pair.of("test", "third"))).toLoggedEvent(),
                Event.event("audio", fieldParam(3, Pair.of("test", "first"))).toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isFalse();
    }

    @Test
    public void matchesEqualEvents() throws Exception {
        Event event = Event.event("audio", tsParam(1));

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                event.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                event.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isTrue();
    }

    @Test
    public void matchesEventParamsByRegex() throws Exception {

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                Event.event("audio", fieldParam(Pair.of(KEY_TIMESTAMP, "14.*7"))).toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                Event.event("audio", fieldParam(Pair.of(KEY_TIMESTAMP, "1415969779287"))).toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isTrue();
    }

    @Test
    public void matchesNestedMaps() throws Exception {
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> subParams = new HashMap<>();
        subParams.put("attr", "test");
        params.put("ts", 1);
        params.put("subparams", subParams);

        Event event = Event.event("audio", params);

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                event.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                event.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isTrue();
    }

    @Test
    public void failsMatchingEventsWithDifferentName() throws Exception {
        Event audio = Event.event("audio", tsParam(1));
        Event click = Event.event("click", tsParam(1));

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                audio.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                click.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isFalse();
    }

    @Test
    public void failsMatchingEventsWithDifferentParams() throws Exception {
        Event audio1 = Event.event("audio", tsParam(1));
        Event audio2 = Event.event("audio", tsParam(2));

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                audio1.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                audio2.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isFalse();
    }

    @Test
    public void failsMatchingEventsWithExtraParams() throws Exception {
        Event audio1 = Event.event("audio", tsParam(1));
        Event audio2 = Event.event("audio", fieldParam(1, Pair.of("extra", "extra")));

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                audio1.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                audio2.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isFalse();
    }

    @Test
    public void failsMatchingEventsWithMissingParams() throws Exception {
        Event actual = Event.event("audio", tsParam(1));
        Event expected = Event.event("audio", fieldParam(1, Pair.of("extra", "extra")));

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                expected.toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                actual.toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isFalse();
    }

    @Test
    public void failsMatchingEventsWithDifferentNestedMaps() throws Exception {
        Map<String, Object> params1 = new HashMap<>();
        Map<String, Object> subParams1 = new HashMap<>();
        subParams1.put("attr", "test");
        params1.put("ts", 1);
        params1.put("subparams", subParams1);

        Map<String, Object> params2 = new HashMap<>();
        Map<String, Object> subParams2 = new HashMap<>();
        subParams2.put("attr2", "test2");
        params2.put("ts", 1);
        params2.put("subparams", subParams2);

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                Event.event("audio", params1).toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                Event.event("audio", params2).toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isFalse();
    }

    @Test
    public void failsMatchingEventsWithNestedMapsVsNormalField() throws Exception {
        Map<String, Object> params1 = new HashMap<>();
        Map<String, Object> subParams1 = new HashMap<>();
        subParams1.put("attr", "test");
        params1.put("ts", 1);
        params1.put("subparams", subParams1);

        Map<String, Object> params2 = new HashMap<>();
        params2.put("ts", 1);
        params2.put("subparams", "NotAMap");

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                Event.event("audio", params1).toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                Event.event("audio", params2).toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isFalse();
    }

    @Test
    public void failsMatchingEventsWithNormalFieldVsNestedMap() throws Exception {
        Map<String, Object> params1 = new HashMap<>();
        Map<String, Object> subParams1 = new HashMap<>();
        subParams1.put("attr", "test");
        params1.put("ts", 1);
        params1.put("subparams", subParams1);

        Map<String, Object> params2 = new HashMap<>();
        params2.put("ts", 1);
        params2.put("subparams", "NotAMap");

        List<String> whiteListedEvents = Arrays.asList();
        List<SpecEvent> expectedEvents = Arrays.asList(
                Event.event("audio", params2).toSpecEvent());
        List<LoggedEvent> events = Arrays.asList(
                Event.event("audio", params1).toLoggedEvent());

        assertThat(validate(whiteListedEvents, expectedEvents, events)).isFalse();
    }

    private boolean validate(List<String> whiteListedEvents, List<SpecEvent> expectedEvents, List<LoggedEvent> events) throws MrLocalLocalException {
        Spec spec = Spec.create(whiteListedEvents, expectedEvents);
        return specValidator.verify(spec, events, 0).wasSuccessful();
    }

    private static Map<String, Object> tsParam(long ts) {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_TIMESTAMP, ts);

        return params;
    }

    private static Map<String, Object> fieldParam(long ts, Pair<String, String>... pairs) {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_TIMESTAMP, ts);
        for (Pair<String, String> pair : pairs) {
            params.put(pair.first(), pair.second());
        }

        return params;
    }

    private static Map<String, Object> fieldParam(Pair<String, String>... pairs) {
        Map<String, Object> params = new HashMap<>();
        for (Pair<String, String> pair : pairs) {
            params.put(pair.first(), pair.second());
        }

        return params;
    }

    private static class Event {
        private final String name;
        private final Map<String, Object> params;
        private final boolean optional;
        private final String version = "v0.0.0";

        private Event(String name, Map<String, Object> params, boolean optional) {
            this.name = name;
            this.params = params;
            this.optional = optional;
        }

        public static Event event(String name, Map<String, Object> params) {
            return new Event(name, params, false);
        }

        public static Event optionalEvent(String name, Map<String, Object> params) {
            return new Event(name, params, true);
        }

        SpecEvent toSpecEvent() {
            return SpecEvent.create(name, optional, params, version);
        }

        LoggedEvent toLoggedEvent() {
            return LoggedEvent.create(name, params, version);
        }
    }
}
