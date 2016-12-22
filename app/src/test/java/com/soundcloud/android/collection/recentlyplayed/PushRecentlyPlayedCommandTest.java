package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.model.ApiRecentlyPlayed;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.matchers.ApiRequestTo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

public class PushRecentlyPlayedCommandTest extends AndroidUnitTest {

    private static final List<PlayHistoryRecord> UN_SYNCED_RECENTLY_PLAYED =
            singletonList(PlayHistoryRecord.create(1000L, Urn.NOT_SET, Urn.forPlaylist(123L)));

    private static final ModelCollection<ApiRecentlyPlayed> API_RECENTLY_PLAYED_COLLECTION =
            new ModelCollection<>(singletonList(ApiRecentlyPlayed.create(1000L, "soundcloud:playlists:123")));

    private static final ApiRequestTo POST_REQUEST =
            isApiRequestTo("POST", ApiEndpoints.RECENTLY_PLAYED.path()).withContent(API_RECENTLY_PLAYED_COLLECTION);

    private static final ApiResponse SUCCESS_RESPONSE = new ApiResponse(null, 201, "");
    private static final ApiResponse ERROR_RESPONSE = new ApiResponse(null, 500, "");

    @Mock private RecentlyPlayedStorage recentlyPlayedStorage;
    @Mock private ApiClient apiClient;

    private PushRecentlyPlayedCommand command;

    @Before
    public void setUp() throws Exception {
        when(recentlyPlayedStorage.loadUnSyncedRecentlyPlayed()).thenReturn(UN_SYNCED_RECENTLY_PLAYED);

        command = new PushRecentlyPlayedCommand(recentlyPlayedStorage, apiClient);
    }

    @Test
    public void shouldPushUnSyncedRecentlyPlayedToServerAndUpdateLocalStorageOnSuccess() throws Exception {
        when(apiClient.fetchResponse(argThat(POST_REQUEST))).thenReturn(SUCCESS_RESPONSE);

        command.call();

        verify(recentlyPlayedStorage).setSynced(UN_SYNCED_RECENTLY_PLAYED);
    }

    @Test
    public void shouldNotFlagLocalStorageAsSyncedOnError() throws Exception {
        when(apiClient.fetchResponse(argThat(POST_REQUEST))).thenReturn(ERROR_RESPONSE);

        command.call();

        verify(recentlyPlayedStorage, never()).setSynced(UN_SYNCED_RECENTLY_PLAYED);
    }

}
