package com.soundcloud.android.discovery.systemplaylist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.IOException;

public class SystemPlaylistOperationsTest extends AndroidUnitTest {
    private static final Urn URN = Urn.forSystemPlaylist(123L);

    @Mock ApiClientRxV2 apiClient;
    @Mock StoreTracksCommand storeTracksCommand;

    private SystemPlaylistOperations operations;

    @Before
    public void setUp() throws Exception {
        apiReturns(Single.never());

        this.operations = new SystemPlaylistOperations(apiClient, storeTracksCommand, Schedulers.trampoline());
    }

    @Test
    public void putsUrnInRequestUrl() throws Exception {
        operations.fetchSystemPlaylist(URN).test();

        ArgumentCaptor<ApiRequest> requestCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClient).mappedResponse(requestCaptor.capture(), eq(ApiSystemPlaylist.class));

        assertThat(requestCaptor.getValue().getUri().toString()).contains(ApiEndpoints.SYSTEM_PLAYLISTS.path(URN));
    }

    @Test
    public void failsOnFailure() throws Exception {
        apiReturns(Single.error(new IOException()));

        operations.fetchSystemPlaylist(URN).test()
                  .assertNoValues()
                  .assertError(IOException.class);

        verify(storeTracksCommand, never()).call();
    }

    @Test
    public void succeedsOnSuccess() throws Exception {
        ApiSystemPlaylist apiSystemPlaylist = ModelFixtures.apiSystemPlaylist();
        apiReturns(Single.just(apiSystemPlaylist));

        operations.fetchSystemPlaylist(URN).test()
                  .assertValueCount(1)
                  .assertNoErrors();

        verify(storeTracksCommand).call(any());
    }

    private void apiReturns(Single<ApiSystemPlaylist> response) {
        when(apiClient.mappedResponse(any(ApiRequest.class), eq(ApiSystemPlaylist.class))).thenReturn(response);
    }
}
