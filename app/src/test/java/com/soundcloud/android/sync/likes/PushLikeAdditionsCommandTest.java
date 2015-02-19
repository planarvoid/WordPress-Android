package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PushLikeAdditionsCommandTest {

    @Test
    public void shouldSpecifyCorrectTypeForJsonParser() {
        PushLikeAdditionsCommand command = new PushLikeAdditionsCommand(mock(ApiClient.class), ApiEndpoints.CREATE_TRACK_LIKES);
        final ApiRequest builder = command.requestBuilder(ApiEndpoints.CREATE_TRACK_LIKES).forPrivateApi(1).build();
        expect(builder.getResourceType()).toEqual(new TypeToken<ModelCollection<ApiLike>>() {
        });
    }
}