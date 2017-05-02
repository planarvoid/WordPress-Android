package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;

@RunWith(JUnit4.class)
public class ApiMultipleContentSelectionCardTest {

    private static final String SELECTION_URN = "soundcloud:selections:the-upload";
    private static final String QUERY_URN = "soundcloud:queries:3rgt3trbg3b3t3rbt3r";
    private static final String STYLE = "go_plus";
    private static final String TITLE = "Playlists for Chilling";
    private static final String DESCRIPTION = "Some \uD83D\uDEC0\uD83C\uDF34\uD83C\uDF0A marketing copy goes here.";
    static final ApiMultipleContentSelectionCard EXPECTED_MULTIPLE_CONTENT_SELECTION_CARD = ApiMultipleContentSelectionCard.create(new Urn(SELECTION_URN),
                                                                                                                                   STYLE,
                                                                                                                                   TITLE,
                                                                                                                                   DESCRIPTION,
                                                                                                                                   new ModelCollection<>(Lists.newArrayList(ApiSelectionItemTest.EXPECTED_PLAYLIST), Collections.emptyMap(), new Urn(QUERY_URN)));
    static final String JSON = "{\n" +
            "  \"selection_urn\": \"" + SELECTION_URN + "\",\n" +
            "  \"style\": \"" + STYLE + "\",\n" +
            "  \"title\": \"" + TITLE + "\",\n" +
            "  \"description\": \"" + DESCRIPTION + "\",\n" +
            "  \"selection_items\": {\n" +
            "    \"collection\": [\n" +
           ApiSelectionItemTest.JSON +
            "    ],\n" +
            "    \"_links\": {},\n" +
            "    \"query_urn\": \"" + QUERY_URN + "\"\n" +
            "  }\n" +
            "}";
    private final JsonTransformer jsonTransformer = new JacksonJsonTransformer();

    @Test
    public void deserialize() throws Exception {
        final ApiMultipleContentSelectionCard fromJson = jsonTransformer.fromJson(JSON, TypeToken.of(ApiMultipleContentSelectionCard.class));

        assertThat(fromJson).isEqualTo(EXPECTED_MULTIPLE_CONTENT_SELECTION_CARD);
    }
}
