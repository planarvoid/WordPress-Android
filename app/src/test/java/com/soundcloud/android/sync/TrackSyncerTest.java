package com.soundcloud.android.sync;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestStorageResults;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;

public class TrackSyncerTest extends AndroidUnitTest {

    @Mock private ApiClient apiClient;
    @Mock private StoreTracksCommand storeTracksCommand;

    @Test
    public void shouldSyncSingleTrack() throws Exception {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", "/tracks/" + track.getUrn().toEncodedString())), eq(ApiTrack.class)))
                .thenReturn(track);
        when(storeTracksCommand.call(singleton(track))).thenReturn(TestStorageResults.successfulInsert());

        TrackSyncer syncer = new TrackSyncer(apiClient, storeTracksCommand);
        ApiSyncResult result = syncer.syncContent(Content.TRACK.forId(track.getId()), Intent.ACTION_SYNC);
        assertThat(result.success).isTrue();
        assertThat(result.synced_at).isGreaterThan(0);
        assertThat(result.change).isEqualTo(ApiSyncResult.CHANGED);
    }

    @Test
    public void shouldFailSyncWithClientErrorIfPersistingTrackFails() throws Exception {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", "/tracks/" + track.getUrn().toEncodedString())), eq(ApiTrack.class)))
                .thenReturn(track);
        when(storeTracksCommand.call(singleton(track))).thenReturn(TestStorageResults.failedInsert());

        TrackSyncer syncer = new TrackSyncer(apiClient, storeTracksCommand);
        ApiSyncResult result = syncer.syncContent(Content.TRACK.forId(track.getId()), Intent.ACTION_SYNC);
        assertThat(result.success).isFalse();
        assertThat(result.synced_at).isEqualTo(0);
        assertThat(result.change).isEqualTo(ApiSyncResult.UNCHANGED);
    }
}
