package com.soundcloud.android.sync.content;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.stream.SoundStreamWriteStorage;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.testsupport.fixtures.ApiStreamItemFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamSyncerTest {

    private static final String NEXT_URL = "some-next-url";
    private static final String NEXT_NEXT_URL = "next-next-url";
    private static final String FUTURE_URL = "future-url";

    private SoundStreamSyncer soundstreamSyncer;

    @Mock private Context context;
    @Mock private ApiClient apiClient;
    @Mock private SoundStreamWriteStorage writeStorage;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;

    private ApiStreamItem streamItem1 = ApiStreamItemFixtures.trackPost();
    private ApiStreamItem streamItem2 = ApiStreamItemFixtures.playlistPost();

    @Captor private ArgumentCaptor<Iterable<ApiStreamItem>> iterableArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);

        soundstreamSyncer = new SoundStreamSyncer(sharedPreferences, apiClient, writeStorage);
    }

    @Test
    public void hardRefreshUsesStorageToReplaceStreamItems() throws Exception {

        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.STREAM.path()))))
                .thenReturn(streamWithoutLinks());

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_HARD_REFRESH);

        verify(writeStorage).replaceStreamItems(iterableArgumentCaptor.capture());
        expect(iterableArgumentCaptor.getValue()).toContainExactly(streamItem1, streamItem2);
    }

    @Test
    public void hardRefreshUsesStorageToReplaceStreamItemsWithPromotedContent() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.STREAM.path()))))
                .thenReturn(new ModelCollection<>(Arrays.asList(streamItem1, ApiStreamItemFixtures.promotedStreamItemWithoutPromoter())));

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_HARD_REFRESH);

        verify(writeStorage).replaceStreamItems(iterableArgumentCaptor.capture());
        expect(iterableArgumentCaptor.getValue()).toContainExactly(streamItem1);
    }

    @Test
    public void hardRefreshStoresTheNextPageUrl() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.STREAM.path()))))
                .thenReturn(streamWithNextLink());

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_HARD_REFRESH);
        verify(sharedPreferencesEditor).putString(SoundStreamSyncer.PREFS_NEXT_URL, NEXT_URL);
    }

    @Test
    public void hardRefreshStoresTheFuturePageUrlIfItExists() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.STREAM.path()))))
                .thenReturn(streamWithFutureLink());

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_HARD_REFRESH);
        verify(sharedPreferencesEditor).putString(SoundStreamSyncer.PREFS_FUTURE_URL, FUTURE_URL);
    }

    @Test
    public void hardRefreshWithoutNextPageUrlClearsNextPageUrl() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.STREAM.path()))))
                .thenReturn(streamWithoutLinks());

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_HARD_REFRESH);
        verify(sharedPreferencesEditor).remove(SoundStreamSyncer.PREFS_NEXT_URL);
    }

    @Test
    public void hardRefreshReturnsSuccessfulChangedResult() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.STREAM.path()))))
                .thenReturn(streamWithoutLinks());

        final ApiSyncResult apiSyncResult = soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_HARD_REFRESH);
        expect(apiSyncResult.success).toBeTrue();
        expect(apiSyncResult.change).toEqual(ApiSyncResult.CHANGED);
    }

    @Test
    public void appendUsesStorageToInsertStreamItems() throws Exception {
        when(sharedPreferences.contains(SoundStreamSyncer.PREFS_NEXT_URL)).thenReturn(true);
        when(sharedPreferences.getString(eq(SoundStreamSyncer.PREFS_NEXT_URL), anyString())).thenReturn(NEXT_URL);
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", NEXT_URL))))
                .thenReturn(streamWithoutLinks());

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_APPEND);

        verify(writeStorage).insertStreamItems(iterableArgumentCaptor.capture());
        expect(iterableArgumentCaptor.getValue()).toContainExactly(streamItem1, streamItem2);
    }

    @Test
    public void appendReturnsSuccessResult() throws Exception {
        when(sharedPreferences.contains(SoundStreamSyncer.PREFS_NEXT_URL)).thenReturn(true);
        when(sharedPreferences.getString(eq(SoundStreamSyncer.PREFS_NEXT_URL), anyString())).thenReturn(NEXT_URL);
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", NEXT_URL))))
                .thenReturn(streamWithoutLinks());

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_APPEND);

        final ApiSyncResult apiSyncResult = soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_APPEND);
        expect(apiSyncResult.success).toBeTrue();
        expect(apiSyncResult.change).toEqual(ApiSyncResult.CHANGED);
    }

    @Test
    public void appendReturnsUnchangedResultWhenEmptyCollectionReturned() throws Exception {
        when(sharedPreferences.contains(SoundStreamSyncer.PREFS_NEXT_URL)).thenReturn(true);
        when(sharedPreferences.getString(eq(SoundStreamSyncer.PREFS_NEXT_URL), anyString())).thenReturn(NEXT_URL);
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", NEXT_URL))))
                .thenReturn(new ModelCollection<>(new ArrayList()));

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_APPEND);

        final ApiSyncResult apiSyncResult = soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_APPEND);
        expect(apiSyncResult.success).toBeTrue();
        expect(apiSyncResult.change).toEqual(ApiSyncResult.UNCHANGED);
    }

    @Test
    public void appendReturnsUnchangedResultWhenNoNextUrlPresent() throws Exception {
        when(sharedPreferences.contains(SoundStreamSyncer.PREFS_NEXT_URL)).thenReturn(false);
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", NEXT_URL))))
                .thenReturn(streamWithoutLinks());

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_APPEND);

        final ApiSyncResult apiSyncResult = soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_APPEND);
        expect(apiSyncResult.success).toBeTrue();
        expect(apiSyncResult.change).toEqual(ApiSyncResult.UNCHANGED);
    }

    @Test
    public void appendStoresTheNextPageUrl() throws Exception {
        when(sharedPreferences.contains(SoundStreamSyncer.PREFS_NEXT_URL)).thenReturn(true);
        when(sharedPreferences.getString(eq(SoundStreamSyncer.PREFS_NEXT_URL), anyString())).thenReturn(NEXT_URL);
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", NEXT_URL))))
                .thenReturn(streamWithNextLink(NEXT_NEXT_URL));

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_APPEND);
        verify(sharedPreferencesEditor).putString(SoundStreamSyncer.PREFS_NEXT_URL, NEXT_NEXT_URL);
    }

    @Test
    public void appendWithoutNextPageUrlClearsNextPageUrl() throws Exception {
        when(sharedPreferences.contains(SoundStreamSyncer.PREFS_NEXT_URL)).thenReturn(true);
        when(sharedPreferences.getString(eq(SoundStreamSyncer.PREFS_NEXT_URL), anyString())).thenReturn(NEXT_URL);
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", NEXT_URL))))
                .thenReturn(streamWithoutLinks());

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_APPEND);
        verify(sharedPreferencesEditor).remove(SoundStreamSyncer.PREFS_NEXT_URL);
    }

    @Test
    public void softRefreshWithoutFutureUrlStoresNewFutureUrl() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.STREAM.path()))))
                .thenReturn(streamWithFutureLink());

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, null);
        verify(sharedPreferencesEditor).putString(SoundStreamSyncer.PREFS_FUTURE_URL, FUTURE_URL);
    }

    @Test
    public void softRefreshWithFutureUrlInsertsNewStreamItems() throws Exception {
        when(sharedPreferences.contains(SoundStreamSyncer.PREFS_FUTURE_URL)).thenReturn(true);
        when(sharedPreferences.getString(eq(SoundStreamSyncer.PREFS_FUTURE_URL), anyString())).thenReturn(FUTURE_URL);

        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", FUTURE_URL))))
                .thenReturn(streamWithoutLinks());

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, null);

        verify(writeStorage).insertStreamItems(iterableArgumentCaptor.capture());
        expect(iterableArgumentCaptor.getValue()).toContainExactly(streamItem1, streamItem2);
    }

    @Test
    public void softRefreshWithFutureUrlInsertsNewStreamItemsWithPromotedContent() throws Exception {
        when(sharedPreferences.contains(SoundStreamSyncer.PREFS_FUTURE_URL)).thenReturn(true);
        when(sharedPreferences.getString(eq(SoundStreamSyncer.PREFS_FUTURE_URL), anyString())).thenReturn(FUTURE_URL);

        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", FUTURE_URL))))
                .thenReturn(new ModelCollection<>(Arrays.asList(streamItem1, ApiStreamItemFixtures.promotedStreamItemWithoutPromoter())));

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, null);

        verify(writeStorage).insertStreamItems(iterableArgumentCaptor.capture());
        expect(iterableArgumentCaptor.getValue()).toContainExactly(streamItem1);
    }

    @Test
    public void softRefreshWithFutureUrlSetsNewFutureUrl() throws Exception {
        when(sharedPreferences.contains(SoundStreamSyncer.PREFS_FUTURE_URL)).thenReturn(true);
        when(sharedPreferences.getString(eq(SoundStreamSyncer.PREFS_FUTURE_URL), anyString())).thenReturn(FUTURE_URL);

        when(apiClient.fetchMappedResponse(argThat(isMobileApiRequestTo("GET", FUTURE_URL))))
                .thenReturn(streamWithFutureLink());

        soundstreamSyncer.syncContent(Content.ME_SOUND_STREAM.uri, null);
        verify(sharedPreferencesEditor).putString(SoundStreamSyncer.PREFS_FUTURE_URL, FUTURE_URL);
    }

    private ModelCollection<ApiStreamItem> streamWithoutLinks() {
        return new ModelCollection<>(Arrays.asList(streamItem1, streamItem2));
    }

    private ModelCollection<ApiStreamItem> streamWithNextLink() {
        return streamWithNextLink(NEXT_URL);
    }

    private ModelCollection<ApiStreamItem> streamWithNextLink(String nextUrl) {
        final ModelCollection<ApiStreamItem> refreshedStream = streamWithoutLinks();
        final HashMap<String, Link> links = new HashMap<>();
        links.put(ModelCollection.NEXT_LINK_REL, new Link(nextUrl));
        refreshedStream.setLinks(links);
        return refreshedStream;
    }

    private ModelCollection<ApiStreamItem> streamWithFutureLink() {
        final ModelCollection<ApiStreamItem> refreshedStream = streamWithoutLinks();
        final HashMap<String, Link> links = new HashMap<>();
        links.put(SoundStreamSyncer.FUTURE_LINK_REL, new Link(FUTURE_URL));
        refreshedStream.setLinks(links);
        return refreshedStream;
    }

}