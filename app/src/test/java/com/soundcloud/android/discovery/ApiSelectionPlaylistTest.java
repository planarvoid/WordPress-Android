package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ApiSelectionPlaylistTest {

    private static final String ARTWORK = "https://i1.sndcdn.com/artworks-000148470532-wq5g7k-{size}.jpg";
    private static final int TRACK_COUNT = 9;
    private static final String SHORT_TITLE = "Chill Playlist";
    private static final String SHORT_SUBTITLE = "willywacker";
    private static final String URN_VALUE = "soundcloud:playlists:96836877";
    private static final Urn URN = new Urn(URN_VALUE);

    static final ApiSelectionPlaylist EXPECTED_PLAYLIST = ApiSelectionPlaylist.create(URN, ARTWORK, TRACK_COUNT, SHORT_TITLE, SHORT_SUBTITLE);
    static final String JSON = "{\"urn\": \"" + URN_VALUE + "\",\"artwork_url_template\": \"" + ARTWORK + "\",\"track_count\": " + TRACK_COUNT + ",\"short_title\": \"" + SHORT_TITLE + "\",\"short_subtitle\": \"" + SHORT_SUBTITLE + "\"}";

    private final JsonTransformer jsonTransformer = new JacksonJsonTransformer();

    @Test
    public void deserialize() throws Exception {
        final ApiSelectionPlaylist fromJson = jsonTransformer.fromJson(JSON, TypeToken.of(ApiSelectionPlaylist.class));

        assertThat(fromJson).isEqualTo(EXPECTED_PLAYLIST);
    }
}
