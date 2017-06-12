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

@RunWith(JUnit4.class)
public class ApiDiscoveryCardTest {

    static final Urn PARENT_QUERY_URN = new Urn("soundcloud:discovery:123");
    static final ApiDiscoveryCard EXPECTED_MULTIPLE_CONTENT_SELECTION_CARD = ApiDiscoveryCard.create(null, ApiMultipleContentSelectionCardTest.EXPECTED_MULTIPLE_CONTENT_SELECTION_CARD);
    static final ApiDiscoveryCard EXPECTED_SINGLE_CONTENT_SELECTION_CARD = ApiDiscoveryCard.create(ApiSingleContentSelectionCardTest.EXPECTED_SINGLE_CONTENT_SELECTION_CARD, null);

    private static final String MULTIPLE_CONTENT_SELECTION_JSON = "{\"multiple_content_selection_card\":" + ApiMultipleContentSelectionCardTest.JSON + "}";
    private static final String SINGLE_CONTENT_SELECTION_JSON = "{\"single_content_selection_card\":" + ApiSingleContentSelectionCardTest.JSON + "}";
    private static final String COLLECTION_JSON = "{\"collection\": [" + SINGLE_CONTENT_SELECTION_JSON + "," + MULTIPLE_CONTENT_SELECTION_JSON + "],\"_links\": {}}";
    private static final ModelCollection<ApiDiscoveryCard> EXPECTED_COLLECTION = new ModelCollection<>(Lists.newArrayList(EXPECTED_SINGLE_CONTENT_SELECTION_CARD, EXPECTED_MULTIPLE_CONTENT_SELECTION_CARD));

    private final JsonTransformer jsonTransformer = new JacksonJsonTransformer();

    @Test
    public void deserializeSelectionCard() throws Exception {
        final ApiDiscoveryCard fromJson = jsonTransformer.fromJson(MULTIPLE_CONTENT_SELECTION_JSON, TypeToken.of(ApiDiscoveryCard.class));

        assertThat(fromJson).isEqualTo(EXPECTED_MULTIPLE_CONTENT_SELECTION_CARD);
    }

    @Test
    public void deserializeSingletonSelectionCard() throws Exception {
        final ApiDiscoveryCard fromJson = jsonTransformer.fromJson(SINGLE_CONTENT_SELECTION_JSON, TypeToken.of(ApiDiscoveryCard.class));

        assertThat(fromJson).isEqualTo(EXPECTED_SINGLE_CONTENT_SELECTION_CARD);
    }

    @Test
    public void deserializeCollection() throws Exception {
        final ModelCollection<ApiDiscoveryCard> fromJson = jsonTransformer.fromJson(COLLECTION_JSON, new TypeToken<ModelCollection<ApiDiscoveryCard>>() {});

        assertThat(fromJson).isEqualTo(EXPECTED_COLLECTION);
    }
}
