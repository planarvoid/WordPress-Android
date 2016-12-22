package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.model.ApiPlayHistory;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.matchers.ApiRequestTo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

public class PushPlayHistoryCommandTest extends AndroidUnitTest {

    private static final List<PlayHistoryRecord> UN_SYNCED_PLAY_HISTORY =
            Collections.singletonList(PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.NOT_SET));

    private static final ModelCollection<ApiPlayHistory> API_PLAY_HISTORY_COLLECTION =
            new ModelCollection<>(Collections.singletonList(ApiPlayHistory.create(1000L, "soundcloud:tracks:123")));

    private static final ApiRequestTo POST_REQUEST =
            isApiRequestTo("POST", ApiEndpoints.PLAY_HISTORY.path()).withContent(API_PLAY_HISTORY_COLLECTION);

    private static final ApiResponse SUCCESS_RESPONSE = new ApiResponse(null, 201, "");
    private static final ApiResponse ERROR_RESPONSE = new ApiResponse(null, 500, "");

    @Mock private PlayHistoryStorage playHistoryStorage;
    @Mock private ApiClient apiClient;

    private PushPlayHistoryCommand command;

    @Before
    public void setUp() throws Exception {
        when(playHistoryStorage.loadUnSyncedPlayHistory()).thenReturn(UN_SYNCED_PLAY_HISTORY);

        command = new PushPlayHistoryCommand(playHistoryStorage, apiClient);
    }

    @Test
    public void shouldPushUnSyncedPlayHistoryToServerAndUpdateLocalStorageOnSuccess() throws Exception {
        when(apiClient.fetchResponse(argThat(POST_REQUEST))).thenReturn(SUCCESS_RESPONSE);

        command.call();

        verify(playHistoryStorage).setSynced(UN_SYNCED_PLAY_HISTORY);
    }

    @Test
    public void shouldNotFlagLocalStorageAsSyncedOnError() throws Exception {
        when(apiClient.fetchResponse(argThat(POST_REQUEST))).thenReturn(ERROR_RESPONSE);

        command.call();

        verify(playHistoryStorage, never()).setSynced(UN_SYNCED_PLAY_HISTORY);
    }

}
