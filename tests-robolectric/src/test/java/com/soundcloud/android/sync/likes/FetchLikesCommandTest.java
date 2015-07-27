package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class FetchLikesCommandTest {

    private FetchLikesCommand fetchLikesCommand;

    @Mock private ApiClient apiClient;

    @Before
    public void setUp() throws Exception {
        fetchLikesCommand = new FetchLikesCommand(apiClient);
    }

    @Test
    public void returnsSetOfCurrentUsersLikes() throws Exception {
        final ApiLike apiLike = ModelFixtures.apiTrackLike();
        final ModelCollection<ApiLike> response = new ModelCollection<>(Arrays.asList(apiLike));
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.LIKED_TRACKS.path())), isA(TypeToken.class))).thenReturn(response);

        expect(fetchLikesCommand.with(ApiEndpoints.LIKED_TRACKS).call()).toContainExactly(apiLike.toPropertySet());
    }
}