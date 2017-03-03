package com.soundcloud.android.api.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class JacksonJsonTransformerTest {

    private ObjectMapper objectMapper = JacksonJsonTransformer.buildObjectMapper();

    private static class WithOptional {
        public Optional<String> optional;

        WithOptional(@JsonProperty("optional") Optional<String> optional) {
            this.optional = optional;
        }
    }

    private static class WithDate {
        public Date date;

        WithDate(@JsonProperty("date") Date date) {
            this.date = date;
        }
    }

    @Test
    public void deserializePresentOptional() throws Exception {
        WithOptional withOptional = objectMapper.readValue("{\"optional\":\"bar\"}", WithOptional.class);
        assertThat(withOptional.optional.isPresent()).isTrue();
        assertThat(withOptional.optional.get()).isEqualTo("bar");
    }

    @Test
    public void deserializeAbsentOptionalFromNull() throws Exception {
        WithOptional withOptional = objectMapper.readValue("{\"optional\":null}", WithOptional.class);
        assertThat(withOptional.optional.isPresent()).isFalse();
    }

    @Test
    public void deserializeAbsentOptionalFromMissing() throws Exception {
        WithOptional withOptional = objectMapper.readValue("{}", WithOptional.class);
        assertThat(withOptional.optional.isPresent()).isFalse();
    }

    @Test
    public void serializePresentOptional() throws Exception {
        WithOptional withOptional = new WithOptional(Optional.of("bar"));
        String json = objectMapper.writeValueAsString(withOptional);
        assertThat(json).isEqualTo("{\"optional\":\"bar\"}");
    }

    @Test
    public void serializeAbsentOptional() throws Exception {
        WithOptional withOptional = new WithOptional(Optional.absent());
        String json = objectMapper.writeValueAsString(withOptional);
        assertThat(json).isEqualTo("{\"optional\":null}");
    }

    @Test
    public void serializeDate() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, 1, 1, 1, 1, 1);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        WithDate withDate = new WithDate(calendar.getTime());
        String json = objectMapper.writeValueAsString(withDate);
        assertThat(json).isEqualTo("{\"date\":\"2000/02/01 01:01:01 +0000\"}");
    }

    @Test
    public void deserializeDate() throws Exception {
        WithDate withDate = objectMapper.readValue("{\"date\":\"2000/02/01 01:01:01 +0000\"}", WithDate.class);
        Calendar actual = Calendar.getInstance();
        actual.setTime(withDate.date);

        Calendar expectedUtc = Calendar.getInstance();
        expectedUtc.set(2000, 1, 1, 1, 1, 1);
        expectedUtc.setTimeZone(TimeZone.getTimeZone("UTC"));

        Calendar expectedInCurrentTimeZone = Calendar.getInstance();
        expectedInCurrentTimeZone.setTimeZone(actual.getTimeZone());
        expectedInCurrentTimeZone.setTimeInMillis(expectedUtc.getTimeInMillis());

        assertThat(actual.get(Calendar.YEAR)).isEqualTo(expectedInCurrentTimeZone.get(Calendar.YEAR));
        assertThat(actual.get(Calendar.DAY_OF_YEAR)).isEqualTo(expectedInCurrentTimeZone.get(Calendar.DAY_OF_YEAR));
        assertThat(actual.get(Calendar.HOUR_OF_DAY)).isEqualTo(expectedInCurrentTimeZone.get(Calendar.HOUR_OF_DAY));
        assertThat(actual.get(Calendar.MINUTE)).isEqualTo(expectedInCurrentTimeZone.get(Calendar.MINUTE));
        assertThat(actual.get(Calendar.SECOND)).isEqualTo(expectedInCurrentTimeZone.get(Calendar.SECOND));
    }

    @Test
    public void deserializePublicApiUser() throws Exception {
        String json = "{\n" +
                "  \"id\": 102628335,\n" +
                "  \"kind\": \"user\",\n" +
                "  \"permalink\": \"sofia-tester\",\n" +
                "  \"username\": \"Sofia Tester\",\n" +
                "  \"last_modified\": \"2016/08/01 15:06:26 +0000\",\n" +
                "  \"uri\": \"https://api.soundcloud.com/users/102628335\",\n" +
                "  \"permalink_url\": \"http://soundcloud.com/sofia-tester\",\n" +
                "  \"avatar_url\": \"https://i1.sndcdn.com/avatars-000092512816-smoy4u-large.jpg\",\n" +
                "  \"country\": null,\n" +
                "  \"first_name\": \"Sofia\",\n" +
                "  \"last_name\": \"Tester\",\n" +
                "  \"full_name\": \"Sofia Tester\",\n" +
                "  \"description\": null,\n" +
                "  \"city\": null,\n" +
                "  \"discogs_name\": null,\n" +
                "  \"myspace_name\": null,\n" +
                "  \"website\": null,\n" +
                "  \"website_title\": null,\n" +
                "  \"track_count\": 0,\n" +
                "  \"playlist_count\": 1,\n" +
                "  \"online\": false,\n" +
                "  \"plan\": \"Free\",\n" +
                "  \"subscriptions\": [],\n" +
                "  \"likes_count\": 142,\n" +
                "  \"comments_count\": 0,\n" +
                "  \"upload_seconds_left\": 10800,\n" +
                "  \"quota\": {\n" +
                "    \"unlimited_upload_quota\": false,\n" +
                "    \"upload_seconds_used\": 0,\n" +
                "    \"upload_seconds_left\": 10800\n" +
                "  },\n" +
                "  \"private_tracks_count\": 0,\n" +
                "  \"private_playlists_count\": 0,\n" +
                "  \"primary_email_confirmed\": true,\n" +
                "  \"locale\": \"\",\n" +
                "  \"followers_count\": 4,\n" +
                "  \"followings_count\": 0,\n" +
                "  \"public_favorites_count\": 142,\n" +
                "  \"reposts_count\": 45\n" +
                "}";
        assertThat(objectMapper.readValue(json, PublicApiUser.class)).isNotNull();
    }
}
