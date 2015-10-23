package com.soundcloud.android.sync;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

// Android test because of Logger
public class TimelineSyncerTest extends AndroidUnitTest {

    private static final Uri CONTENT_URI = Uri.parse("content://uri");
    private static final String NEXT_URL = "some-next-url";
    private static final String NEXT_NEXT_URL = "next-next-url";
    private static final String FUTURE_URL = "future-url";
    private static final ApiEndpoints ENDPOINT = ApiEndpoints.STREAM;

    private TimelineSyncer timelineSyncer;

    @Mock private Context context;
    @Mock private ApiClient apiClient;
    @Mock private Command<Iterable<Object>, ?> insertCommand;
    @Mock private Command<Iterable<Object>, ?> replaceCommand;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock private TimelineSyncStorage timelineSyncStorage;

    private Object streamItem1 = new Object();
    private Object streamItem2 = new Object();

    @Captor private ArgumentCaptor<Iterable<Object>> iterableArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);

        timelineSyncer = new TimelineSyncer<>(ENDPOINT, CONTENT_URI, apiClient, insertCommand, replaceCommand,
                timelineSyncStorage, new TypeToken<ModelCollection<Object>>() {
        });
    }

    @Test
    public void hardRefreshUsesStorageToReplaceStreamItems() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ENDPOINT.path())), isA(TypeToken.class)))
                .thenReturn(itemsWithoutLinks());

        timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_HARD_REFRESH);

        verify(replaceCommand).call(iterableArgumentCaptor.capture());
        assertThat(iterableArgumentCaptor.getValue()).containsExactly(streamItem1, streamItem2);
    }

    @Test
    public void hardRefreshSetsTheNextPageUrl() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ENDPOINT.path())), isA(TypeToken.class)))
                .thenReturn(itemsWithNextLink());

        timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_HARD_REFRESH);

        verify(timelineSyncStorage).storeNextPageUrl(Optional.of(new Link(NEXT_URL)));
    }

    @Test
    public void hardRefreshSetsTheFuturePageUrl() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ENDPOINT.path())), isA(TypeToken.class)))
                .thenReturn(itemsWithFutureLink());

        timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_HARD_REFRESH);

        verify(timelineSyncStorage).storeFuturePageUrl(new Link(FUTURE_URL));
    }

    @Test
    public void hardRefreshReturnsSuccessfulChangedResult() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ENDPOINT.path())), isA(TypeToken.class)))
                .thenReturn(itemsWithoutLinks());

        final ApiSyncResult apiSyncResult = timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_HARD_REFRESH);
        assertThat(apiSyncResult.success).isTrue();
        assertThat(apiSyncResult.change).isEqualTo(ApiSyncResult.CHANGED);
    }

    @Test
    public void appendUsesStorageToInsertStreamItems() throws Exception {
        when(timelineSyncStorage.hasNextPageUrl()).thenReturn(true);
        when(timelineSyncStorage.getNextPageUrl()).thenReturn(NEXT_URL);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", NEXT_URL)), isA(TypeToken.class)))
                .thenReturn(itemsWithoutLinks());

        timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_APPEND);

        verify(insertCommand).call(iterableArgumentCaptor.capture());
        assertThat(iterableArgumentCaptor.getValue()).containsExactly(streamItem1, streamItem2);
    }

    @Test
    public void appendReturnsSuccessResult() throws Exception {
        when(timelineSyncStorage.hasNextPageUrl()).thenReturn(true);
        when(timelineSyncStorage.getNextPageUrl()).thenReturn(NEXT_URL);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", NEXT_URL)), isA(TypeToken.class)))
                .thenReturn(itemsWithoutLinks());

        timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_APPEND);

        final ApiSyncResult apiSyncResult = timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_APPEND);
        assertThat(apiSyncResult.success).isTrue();
        assertThat(apiSyncResult.change).isEqualTo(ApiSyncResult.CHANGED);
    }

    @Test
    public void appendReturnsUnchangedResultWhenEmptyCollectionReturned() throws Exception {
        when(timelineSyncStorage.hasNextPageUrl()).thenReturn(true);
        when(timelineSyncStorage.getNextPageUrl()).thenReturn(NEXT_URL);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", NEXT_URL)), isA(TypeToken.class)))
                .thenReturn(new ModelCollection<>(new ArrayList<>()));

        timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_APPEND);

        final ApiSyncResult apiSyncResult = timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_APPEND);
        assertThat(apiSyncResult.success).isTrue();
        assertThat(apiSyncResult.change).isEqualTo(ApiSyncResult.UNCHANGED);
    }

    @Test
    public void appendReturnsUnchangedResultWhenNoNextUrlPresent() throws Exception {
        when(timelineSyncStorage.hasNextPageUrl()).thenReturn(false);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", NEXT_URL)), isA(TypeToken.class)))
                .thenReturn(itemsWithoutLinks());

        timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_APPEND);

        final ApiSyncResult apiSyncResult = timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_APPEND);
        assertThat(apiSyncResult.success).isTrue();
        assertThat(apiSyncResult.change).isEqualTo(ApiSyncResult.UNCHANGED);
    }

    @Test
    public void appendSetsTheNextPageUrl() throws Exception {
        when(timelineSyncStorage.hasNextPageUrl()).thenReturn(true);
        when(timelineSyncStorage.getNextPageUrl()).thenReturn(NEXT_URL);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", NEXT_URL)), isA(TypeToken.class)))
                .thenReturn(itemsWithNextLink(NEXT_NEXT_URL));

        timelineSyncer.syncContent(CONTENT_URI, ApiSyncService.ACTION_APPEND);

        verify(timelineSyncStorage).storeNextPageUrl(Optional.of(new Link(NEXT_NEXT_URL)));
    }

    @Test
    public void softRefreshResultingIn400ClearsFutureLinkAndReportsError() throws Exception {
        when(timelineSyncStorage.isMissingFuturePageUrl()).thenReturn(false);
        when(timelineSyncStorage.getFuturePageUrl()).thenReturn(FUTURE_URL);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", FUTURE_URL)), isA(TypeToken.class)))
                .thenThrow(ApiRequestException.badRequest(null, null, null));

        try {
            timelineSyncer.syncContent(CONTENT_URI, null);
            fail("Expected exception after API 400");
        } catch (ApiRequestException e) {
            assertThat(e.reason()).isEqualTo(ApiRequestException.Reason.BAD_REQUEST);
        }

        verify(timelineSyncStorage).clear();
    }

    @Test
    public void softRefreshResultingIn404ClearsFutureLinkAndReportsError() throws Exception {
        when(timelineSyncStorage.isMissingFuturePageUrl()).thenReturn(false);
        when(timelineSyncStorage.getFuturePageUrl()).thenReturn(FUTURE_URL);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", FUTURE_URL)), isA(TypeToken.class)))
                .thenThrow(ApiRequestException.notFound(null, null));

        try {
            timelineSyncer.syncContent(CONTENT_URI, null);
            fail("Expected exception after API 404");
        } catch (ApiRequestException e) {
            assertThat(e.reason()).isEqualTo(ApiRequestException.Reason.NOT_FOUND);
        }

        verify(timelineSyncStorage).clear();
    }

    @Test
    public void softRefreshResultingIn500ClearsFutureLinkAndReportsError() throws Exception {
        when(timelineSyncStorage.isMissingFuturePageUrl()).thenReturn(false);
        when(timelineSyncStorage.getFuturePageUrl()).thenReturn(FUTURE_URL);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", FUTURE_URL)), isA(TypeToken.class)))
                .thenThrow(ApiRequestException.serverError(null, null));

        try {
            timelineSyncer.syncContent(CONTENT_URI, null);
            fail("Expected exception after API 500");
        } catch (ApiRequestException e) {
            assertThat(e.reason()).isEqualTo(ApiRequestException.Reason.SERVER_ERROR);
        }

        verify(timelineSyncStorage).clear();
    }

    @Test
    public void softRefreshWithoutFutureUrlStoresNewFutureUrl() throws Exception {
        when(timelineSyncStorage.isMissingFuturePageUrl()).thenReturn(true);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ENDPOINT.path())), isA(TypeToken.class)))
                .thenReturn(itemsWithFutureLink());

        timelineSyncer.syncContent(CONTENT_URI, null);

        verify(timelineSyncStorage).storeFuturePageUrl(new Link(FUTURE_URL));
    }

    @Test
    public void softRefreshWithFutureUrlInsertsNewStreamItems() throws Exception {
        when(timelineSyncStorage.isMissingFuturePageUrl()).thenReturn(false);
        when(timelineSyncStorage.getFuturePageUrl()).thenReturn(FUTURE_URL);

        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", FUTURE_URL)), isA(TypeToken.class)))
                .thenReturn(itemsWithoutLinks());

        timelineSyncer.syncContent(CONTENT_URI, null);

        verify(insertCommand).call(iterableArgumentCaptor.capture());
        assertThat(iterableArgumentCaptor.getValue()).containsExactly(streamItem1, streamItem2);
    }

    @Test
    public void softRefreshWithFutureUrlSetsNewFutureUrl() throws Exception {
        when(timelineSyncStorage.isMissingFuturePageUrl()).thenReturn(false);
        when(timelineSyncStorage.getFuturePageUrl()).thenReturn(FUTURE_URL);

        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", FUTURE_URL)), isA(TypeToken.class)))
                .thenReturn(itemsWithFutureLink());

        timelineSyncer.syncContent(CONTENT_URI, null);

        verify(timelineSyncStorage).storeFuturePageUrl(new Link(FUTURE_URL));
    }

    private ModelCollection<?> itemsWithoutLinks() {
        return new ModelCollection<>(Arrays.asList(streamItem1, streamItem2));
    }

    private ModelCollection<?> itemsWithNextLink() {
        return itemsWithNextLink(NEXT_URL);
    }

    private ModelCollection<?> itemsWithNextLink(String nextUrl) {
        final ModelCollection<?> items = itemsWithoutLinks();
        final HashMap<String, Link> links = new HashMap<>();
        links.put(ModelCollection.NEXT_LINK_REL, new Link(nextUrl));
        items.setLinks(links);
        return items;
    }

    private ModelCollection<?> itemsWithFutureLink() {
        final ModelCollection<?> refreshedStream = itemsWithoutLinks();
        final HashMap<String, Link> links = new HashMap<>();
        links.put(TimelineSyncer.FUTURE_LINK_REL, new Link(FUTURE_URL));
        refreshedStream.setLinks(links);
        return refreshedStream;
    }

}
