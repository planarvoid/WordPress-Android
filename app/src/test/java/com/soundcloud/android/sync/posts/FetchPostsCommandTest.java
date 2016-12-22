package com.soundcloud.android.sync.posts;

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

public class FetchPostsCommandTest extends AndroidUnitTest {

    private FetchPostsCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setup() {
        command = new FetchPostsCommand(apiClient);
    }

    @Test
    public void fetchesPostsViaApiMobile() throws Exception {
        final ApiPostItem apiTrackPostItem = ModelFixtures.apiTrackPostItem();
        final ModelCollection<ApiPostItem> response = new ModelCollection<>(singletonList(apiTrackPostItem));
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.MY_PLAYLIST_POSTS.path())), isA(TypeToken.class)))
                .thenReturn(response);

        assertThat(command.with(ApiEndpoints.MY_PLAYLIST_POSTS)
                          .call()).containsExactly(apiTrackPostItem.toPropertySet());
    }
}
