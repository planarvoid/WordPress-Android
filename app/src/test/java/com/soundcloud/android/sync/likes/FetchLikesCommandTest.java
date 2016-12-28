package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
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

public class FetchLikesCommandTest extends AndroidUnitTest {

    private FetchLikesCommand fetchLikesCommand;

    @Mock private ApiClient apiClient;

    @Before
    public void setUp() throws Exception {
        fetchLikesCommand = new FetchLikesCommand(apiClient);
    }

    @Test
    public void returnsSetOfCurrentUsersLikes() throws Exception {
        final ApiLike apiLike = ModelFixtures.apiTrackLike();
        final ModelCollection<ApiLike> response = new ModelCollection<>(singletonList(apiLike));
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.LIKED_TRACKS.path())), isA(TypeToken.class)))
                .thenReturn(response);

        assertThat(fetchLikesCommand.with(ApiEndpoints.LIKED_TRACKS).call()).containsExactly(apiLike);
    }
}
