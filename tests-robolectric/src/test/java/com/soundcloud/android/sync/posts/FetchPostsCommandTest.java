package com.soundcloud.android.sync.posts;

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
public class FetchPostsCommandTest {

    private FetchPostsCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setup() {
        command = new FetchPostsCommand(apiClient);
    }

    @Test
    public void fetchesPostsViaApiMobile() throws Exception {
        final ApiPostItem apiTrackPostItem = ModelFixtures.apiTrackPostItem();
        final ModelCollection<ApiPostItem> response = new ModelCollection<>(Arrays.asList(apiTrackPostItem));
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.MY_PLAYLIST_POSTS.path())), isA(TypeToken.class))).thenReturn(response);

        expect(command.with(ApiEndpoints.MY_PLAYLIST_POSTS).call()).toContainExactly(apiTrackPostItem.toPropertySet());
    }
}