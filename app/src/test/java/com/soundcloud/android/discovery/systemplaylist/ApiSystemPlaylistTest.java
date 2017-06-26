package com.soundcloud.android.discovery.systemplaylist;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.ApiDateFormat;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ApiSystemPlaylistTest {

    private static final String URN = "soundcloud:bal:213";
    private static final int TRACK_COUNT = 50;
    private static final String LAST_UPDATED = "2017/04/25 12:08:02 +0000";
    private static final String TITLE = "Ambient: New & Hot";
    private static final String DESCRIPTION = "Up-and-coming Ambient tracks on SoundCloud.";
    private static final String ARTWORK_URL = "https://i1.sndcdn.com/artworks-000218558811-elkuw1-{size}.jpg";
    private static final String TRACKING_FEATURE_NAME = "The Upload";
    private static final String TRACKS_JSON = "{\"collection\": [], \"_links\": {}}";
    private static final String JSON = "{\"urn\": \"" + URN + "\",\"track_count\": " + TRACK_COUNT + ",\"last_updated\": \"" + LAST_UPDATED + "\",\"title\": \"" + TITLE + "\",\"description\": \"" + DESCRIPTION + "\",\"artwork_url_template\": \"" + ARTWORK_URL + "\",\"tracking_feature_name\": \"" + TRACKING_FEATURE_NAME + "\",\"tracks\": " + TRACKS_JSON + "}";

    private final JsonTransformer jsonTransformer = new JacksonJsonTransformer();

    @Test
    public void deserialize() throws Exception {
        ApiSystemPlaylist EXPECTED = ApiSystemPlaylist.create(new Urn(URN), Optional.of(TRACK_COUNT), Optional.of(new ApiDateFormat().parse(LAST_UPDATED)), Optional.of(TITLE), Optional.of(DESCRIPTION), Optional.of(ARTWORK_URL), Optional.of(TRACKING_FEATURE_NAME), new ModelCollection<>());
        final ApiSystemPlaylist fromJson = jsonTransformer.fromJson(JSON, TypeToken.of(ApiSystemPlaylist.class));

        assertThat(fromJson).isEqualTo(EXPECTED);
    }
}
