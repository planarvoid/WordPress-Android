package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ApiSingletonSelectionCardTest {
    private static final String SELECTION_URN = "soundcloud:selections:new-release:soundcloud:playlists:283891650";
    private static final String QUERY_URN = "soundcloud:queries:vefvefgvhvth334v54v";
    private static final String STYLE = "go_plus";
    private static final String TITLE = "New Release from Little Simz";
    private static final String DESCRIPTION = "Stillness In Wonderland";
    private static final String AVATAR_URL = "https://i1.sndcdn.com/artworks-000136596659-7rdy0i-{size}.jpg";
    static final String JSON = "{\n" +
            "        \"selection_urn\": \"" + SELECTION_URN + "\",\n" +
            "        \"query_urn\": \"" + QUERY_URN + "\",\n" +
            "        \"style\": \"" + STYLE + "\",\n" +
            "        \"title\": \"" + TITLE + "\",\n" +
            "        \"description\": \"" + DESCRIPTION + "\",\n" +
            "        \"selection_playlist\": " + ApiSelectionPlaylistTest.JSON + ",\n" +
            "        \"social_proof_avatar_url_templates\": [\"" + AVATAR_URL + "\"]\n" +
            "      }";
    static final ApiSingletonSelectionCard EXPECTED_SINGLETON_SELECTION_CARD = ApiSingletonSelectionCard.create(new Urn(SELECTION_URN),
                                                                                                                new Urn(QUERY_URN),
                                                                                                                STYLE,
                                                                                                                TITLE,
                                                                                                                DESCRIPTION,
                                                                                                                null,
                                                                                                                ApiSelectionPlaylistTest.EXPECTED_PLAYLIST,
                                                                                                                Lists.newArrayList(AVATAR_URL));
    private final JsonTransformer jsonTransformer = new JacksonJsonTransformer();

    @Test
    public void deserialize() throws Exception {
        final ApiSingletonSelectionCard fromJson = jsonTransformer.fromJson(JSON, TypeToken.of(ApiSingletonSelectionCard.class));

        assertThat(fromJson).isEqualTo(EXPECTED_SINGLETON_SELECTION_CARD);
    }
}
