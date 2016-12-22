package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiRecentlyPlayed;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

public class FetchRecentlyPlayedCommandTest extends AndroidUnitTest {

    private static final Urn PLAYLIST_URN = Urn.forPlaylist(1L);

    private static final ApiRecentlyPlayed API_RECENTLY_PLAYED_ENTRY =
            ApiRecentlyPlayed.create(1000L, PLAYLIST_URN.toString());

    private static final List<ApiRecentlyPlayed> API_RECENTLY_PLAYED = singletonList(API_RECENTLY_PLAYED_ENTRY);
    private static final List<PlayHistoryRecord> RECENTLY_PLAYED_RECORDS =
            singletonList(PlayHistoryRecord.create(1000L, Urn.NOT_SET, PLAYLIST_URN));

    @Mock private ApiClient apiClient;

    private FetchRecentlyPlayedCommand command;

    @Before
    public void setUp() throws Exception {
        command = new FetchRecentlyPlayedCommand(apiClient);
    }

    @Test
    public void shouldReturnRecentlyPlayed() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.RECENTLY_PLAYED.path())),
                any(TypeToken.class))).thenReturn(new ModelCollection<>(API_RECENTLY_PLAYED));

        List<PlayHistoryRecord> result = command.call();

        assertThat(result).isEqualTo(RECENTLY_PLAYED_RECORDS);
    }
}
