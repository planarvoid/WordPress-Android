package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class PushLikesCommandTest extends AndroidUnitTest {

    private PushLikesCommand<ApiLike> pushLikesCommand;

    @Mock private ApiClient apiClient;

    private ApiLike apiLike = ModelFixtures.apiTrackLike();

    @Before
    public void setup() {
        pushLikesCommand = new PushLikesCommand<>(apiClient,
                                                  ApiEndpoints.CREATE_TRACK_LIKES,
                                                  new TypeToken<ModelCollection<ApiLike>>() {
                                                  });
    }

    @Test
    public void shouldPushGivenLikesAndReturnSuccessCollection() throws Exception {
        Map expectedBody = Collections.singletonMap(
                "likes",
                Collections.singletonList(Collections.singletonMap("target_urn", apiLike.getTargetUrn().toString()))
        );
        ModelCollection<ApiLike> addedLikes = new ModelCollection<>(Collections.singletonList(apiLike));

        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.CREATE_TRACK_LIKES.path())
                                                                           .withContent(expectedBody)), isA(TypeToken.class))).thenReturn(addedLikes);

        Collection<ApiLike> result = pushLikesCommand.with(Collections.singleton(apiLike)).call();
        assertThat(result).containsExactly(ApiLike.create(apiLike.getTargetUrn(), apiLike.getCreatedAt()));
    }
}
