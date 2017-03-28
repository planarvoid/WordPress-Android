package com.soundcloud.android.cast.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CastJsonHandlerTest extends AndroidUnitTest {

    private CastJsonHandler castJsonHandler;

    @Before
    public void setUp() {
        JsonTransformer jsonTransformer = new JacksonJsonTransformer();
        castJsonHandler = new CastJsonHandler(jsonTransformer);
    }

    @Test
    public void parsesCastMessage() throws IOException, ApiMapperException, JSONException {
        String type = CastJsonHandler.KEY_QUEUE_STATUS;
        String revision = "rev_1";
        String json = "{\"" + type + "\":{\"revision\":\"" + revision + "\",\"queue\":[],\"current_index\":null,\"source\":null,\"version\":\"1.0.0\"}}";

        CastPlayQueue castPlayQueue = castJsonHandler.parseCastPlayQueue(new JSONObject(json));

        assertThat(castPlayQueue.getRevision()).isEqualTo(revision);
        assertThat(castPlayQueue.getQueue()).isEmpty();
    }

    @Test
    public void marshallsCastPlayQueue() throws JSONException, ApiMapperException {
        Urn urn1 = Urn.forTrack(123L);
        Urn urn2 = Urn.forTrack(456L);
        List<Urn> playQueue = Arrays.asList(urn1, urn2);
        CastPlayQueue castPlayQueue = new CastPlayQueue(urn2, playQueue);

        JSONObject jsonObject = castJsonHandler.toJson(castPlayQueue);

        assertThat(jsonObject.getInt("current_index")).isEqualTo(1);
        assertThat(jsonObject.getString("queue")).contains("tracks:123", "tracks:456");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsForInvalidCastPlayQueueMarshalling() {
        castJsonHandler.toJson(null);
    }

}
