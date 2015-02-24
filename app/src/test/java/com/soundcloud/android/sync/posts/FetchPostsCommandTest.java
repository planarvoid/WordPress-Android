package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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
        final ApiPost apiPost = ModelFixtures.apiTrackPost();
        final ModelCollection<ApiPost> response = new ModelCollection<>(Arrays.asList(apiPost));
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.MY_PLAYLISTS_POSTS.path())))).thenReturn(response);

        expect(command.with(ApiEndpoints.MY_PLAYLISTS_POSTS).call()).toContainExactly(apiPost.toPropertySet());
    }
}