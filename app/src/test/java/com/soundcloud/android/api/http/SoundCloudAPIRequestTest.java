package com.soundcloud.android.api.http;

import static org.junit.Assert.assertEquals;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.model.PlaylistSummaryCollection;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class SoundCloudAPIRequestTest {

    @Test
    public void shouldGetEncodedUriPath() throws Exception {
        APIRequest request = SoundCloudAPIRequest.RequestBuilder.<PlaylistSummaryCollection>get("/someuri/%25+string")
                .forPrivateAPI(1)
                .forResource(TypeToken.of(PlaylistSummaryCollection.class))
                .build();
        assertEquals("/someuri/%25+string", request.getEncodedPath());
    }

}
