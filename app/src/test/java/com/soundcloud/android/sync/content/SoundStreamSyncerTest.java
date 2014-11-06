package com.soundcloud.android.sync.content;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.stream.SoundStreamWriteStorage;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Context;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamSyncerTest {

    private SoundStreamSyncer soundstreamSyncer;

    @Mock private Context context;
    @Mock private ApiClient apiClient;
    @Mock private SoundStreamWriteStorage writeStorage;

    @Mock private ApiStreamItem streamItem1;
    @Mock private ApiStreamItem streamItem2;

    @Captor private ArgumentCaptor<Iterable> iterableArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        soundstreamSyncer = new SoundStreamSyncer(apiClient, writeStorage);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void hardRefreshUsesStorageToReplaceStreamItems() throws Exception {

        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.STREAM.path()))))
                .thenReturn(new ModelCollection<>(Arrays.asList(streamItem1, streamItem2)));

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_HARD_REFRESH);

        verify(writeStorage).replaceStreamItems(iterableArgumentCaptor.capture());
        expect(iterableArgumentCaptor.getValue()).toContainExactly(streamItem1, streamItem2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void hardRefreshUsesStorageToReplaceStreamItemsWithPromotedContent() throws Exception {
        when(streamItem2.isPromotedStreamItem()).thenReturn(true);
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.STREAM.path()))))
                .thenReturn(new ModelCollection<>(Arrays.asList(streamItem1, streamItem2)));

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_HARD_REFRESH);

        verify(writeStorage).replaceStreamItems(iterableArgumentCaptor.capture());
        expect(iterableArgumentCaptor.getValue()).toContainExactly(streamItem1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void hardRefreshReturnsSuccessfulChangedResult() throws Exception {
        when(streamItem2.isPromotedStreamItem()).thenReturn(true);
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.STREAM.path()))))
                .thenReturn(new ModelCollection<>(Arrays.asList(streamItem1, streamItem2)));

        final ApiSyncResult apiSyncResult = soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_HARD_REFRESH);
        expect(apiSyncResult.success).toBeTrue();
        expect(apiSyncResult.change).toEqual(ApiSyncResult.CHANGED);


    }

}