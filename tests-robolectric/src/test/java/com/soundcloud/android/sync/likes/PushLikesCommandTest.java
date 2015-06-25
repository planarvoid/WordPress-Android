package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.likes.PushLikesCommand.AddedLikesCollection;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class PushLikesCommandTest {

    private PushLikesCommand<ApiLike> pushLikesCommand;

    @Mock private ApiClient apiClient;

    private ApiLike apiLike = ModelFixtures.apiTrackLike();
    private PropertySet input = PropertySet.from(LikeProperty.TARGET_URN.bind(apiLike.getTargetUrn()));

    @Before
    public void setup() {
        pushLikesCommand = new PushLikesCommand<>(apiClient, ApiEndpoints.CREATE_TRACK_LIKES, AddedLikesCollection.class);
    }

    @Test
    public void shouldPushGivenLikesAndReturnSuccessCollection() throws Exception {
        Map expectedBody = Collections.singletonMap(
                "likes", Arrays.asList(Collections.singletonMap("target_urn", apiLike.getTargetUrn().toString()))
        );
        ModelCollection<ApiLike> addedLikes = new ModelCollection<>();
        addedLikes.setCollection(Arrays.asList(apiLike));

        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("POST", ApiEndpoints.CREATE_TRACK_LIKES.path())
                        .withContent(expectedBody)), isA(TypeToken.class))).thenReturn(addedLikes);

        Collection<PropertySet> result = pushLikesCommand.with(Collections.singleton(input)).call();
        expect(result).toContainExactly(PropertySet.from(
                LikeProperty.TARGET_URN.bind(apiLike.getTargetUrn()),
                LikeProperty.CREATED_AT.bind(apiLike.getCreatedAt())
        ));
    }
}