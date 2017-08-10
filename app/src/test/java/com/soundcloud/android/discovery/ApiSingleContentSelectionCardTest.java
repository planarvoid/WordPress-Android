package com.soundcloud.android.discovery;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ApiSingleContentSelectionCardTest {
    private static final String SELECTION_URN = "soundcloud:selections:new-release:soundcloud:playlists:283891650";
    private static final String QUERY_URN = "soundcloud:queries:vefvefgvhvth334v54v";
    private static final String STYLE = "go_plus";
    private static final String TITLE = "New Release from Little Simz";
    private static final String DESCRIPTION = "Stillness In Wonderland";
    private static final String TRACKING_FEATURE_NAME = "playlist-discovery";
    private static final String AVATAR_URL = "https://i1.sndcdn.com/artworks-000136596659-7rdy0i-{size}.jpg";
    static final String JSON = "{\n" +
            "        \"selection_urn\": \"" + SELECTION_URN + "\",\n" +
            "        \"query_urn\": \"" + QUERY_URN + "\",\n" +
            "        \"style\": \"" + STYLE + "\",\n" +
            "        \"title\": \"" + TITLE + "\",\n" +
            "        \"description\": \"" + DESCRIPTION + "\",\n" +
            "        \"tracking_feature_name\": \"" + TRACKING_FEATURE_NAME + "\",\n" +
            "        \"selection_item\": " + ApiSelectionItemTest.JSON + ",\n" +
            "        \"social_proof_avatar_url_templates\": [\"" + AVATAR_URL + "\"]\n" +
            "      }";
    static final ApiSingleContentSelectionCard EXPECTED_SINGLE_CONTENT_SELECTION_CARD = ApiSingleContentSelectionCard.create(new Urn(SELECTION_URN),
                                                                                                                             new Urn(QUERY_URN),
                                                                                                                             STYLE,
                                                                                                                             TITLE,
                                                                                                                             DESCRIPTION,
                                                                                                                             null,
                                                                                                                             TRACKING_FEATURE_NAME,
                                                                                                                             ApiSelectionItemTest.EXPECTED_PLAYLIST,
                                                                                                                             Lists.newArrayList(AVATAR_URL));
    private final JsonTransformer jsonTransformer = new JacksonJsonTransformer();

    @Test
    public void deserialize() throws Exception {
        final ApiSingleContentSelectionCard fromJson = jsonTransformer.fromJson(JSON, TypeToken.of(ApiSingleContentSelectionCard.class));

        assertThat(fromJson).isEqualTo(EXPECTED_SINGLE_CONTENT_SELECTION_CARD);
    }
}
