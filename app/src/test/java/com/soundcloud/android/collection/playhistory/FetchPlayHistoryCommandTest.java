package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiPlayHistory;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

public class FetchPlayHistoryCommandTest extends AndroidUnitTest {

    private static final ApiPlayHistory API_PLAY_HISTORY_ENTRY = ApiPlayHistory.create(1000L, "soundcloud:tracks:1");
    private static final List<ApiPlayHistory> API_PLAY_HISTORY = singletonList(API_PLAY_HISTORY_ENTRY);
    private static final List<PlayHistoryRecord> PLAY_HISTORY_RECORDS =
            singletonList(PlayHistoryRecord.create(1000L, Urn.forTrack(1L), Urn.NOT_SET));

    @Mock private ApiClient apiClient;

    private FetchPlayHistoryCommand command;

    @Before
    public void setUp() throws Exception {
        command = new FetchPlayHistoryCommand(apiClient);
    }

    @Test
    public void shouldReturnPlayHistory() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.PLAY_HISTORY.path())),
                any(TypeToken.class))).thenReturn(new ModelCollection<>(API_PLAY_HISTORY));

        List<PlayHistoryRecord> result = command.call();

        assertThat(result).isEqualTo(PLAY_HISTORY_RECORDS);
    }
}
