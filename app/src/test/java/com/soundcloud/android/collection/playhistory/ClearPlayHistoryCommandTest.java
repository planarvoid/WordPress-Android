package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.matchers.ApiRequestTo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ClearPlayHistoryCommandTest extends AndroidUnitTest {

    private static final ApiRequestTo DELETE_REQUEST = isApiRequestTo("DELETE", ApiEndpoints.CLEAR_PLAY_HISTORY.path());

    @Mock private ApiClient apiClient;
    @Mock private PlayHistoryStorage storage;

    private ClearPlayHistoryCommand command;

    @Before
    public void setUp() throws Exception {
        command = new ClearPlayHistoryCommand(storage, apiClient);
    }

    @Test
    public void shouldClearStorageWhenApiCallSucceeds() throws Exception {
        when(apiClient.fetchResponse(argThat(DELETE_REQUEST))).thenReturn(new ApiResponse(null, 201, ""));

        Boolean result = command.call();

        assertThat(result).isTrue();
        verify(storage).clear();
    }

    @Test
    public void shouldNotClearStorageWhenApiCallFails() throws Exception {
        when(apiClient.fetchResponse(argThat(DELETE_REQUEST))).thenReturn(new ApiResponse(null, 500, ""));

        Boolean result = command.call();

        assertThat(result).isFalse();
        verify(storage, never()).clear();
    }

}
