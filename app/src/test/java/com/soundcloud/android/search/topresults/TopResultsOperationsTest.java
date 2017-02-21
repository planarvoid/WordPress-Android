package com.soundcloud.android.search.topresults;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.CacheUniversalSearchCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TopResultsOperationsTest extends AndroidUnitTest {

    private static final String QUERY = "query";
    private static final Urn QUERY_URN = new Urn("soundcloud:query_urn:123");
    private static final Urn TRACKS_BUCKET_URN = new Urn("soundcloud:search-buckets:freetiertracks");
    private static final ApiUniversalSearchItem UNIVERSAL_TRACK_ITEM = new ApiUniversalSearchItem(null, null, ModelFixtures.create(ApiTrack.class));
    private static final TypeToken<ApiTopResults> TYPE_TOKEN = new TypeToken<ApiTopResults>() {
    };

    @Mock private CacheUniversalSearchCommand cacheUniversalSearchCommand;
    @Mock private ApiClientRx apiClientRx;
    @Captor private ArgumentCaptor<ApiRequest> argumentCaptor;

    private TopResultsOperations topResultsOperations;
    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<List<ApiTopResultsBucket>> subscriber;

    @Before
    public void setUp() throws Exception {
        topResultsOperations = new TopResultsOperations(apiClientRx, scheduler, cacheUniversalSearchCommand);
        subscriber = new TestSubscriber<>();
    }

    @Test
    public void performSearchAndCacheResults() throws Exception {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = newArrayList(UNIVERSAL_TRACK_ITEM);
        final ApiTopResultsBucket apiTopResultsBucket = ApiTopResultsBucket.create(TRACKS_BUCKET_URN, 5, new ModelCollection<>(apiUniversalSearchItems, Collections.emptyMap(), QUERY_URN));
        final ModelCollection<ApiTopResultsBucket> buckets = new ModelCollection<>(newArrayList(apiTopResultsBucket), Collections.emptyMap(), QUERY_URN);
        final ApiTopResults apiTopResults = ApiTopResults.create(10, buckets);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(TYPE_TOKEN))).thenReturn(Observable.just(apiTopResults));

        topResultsOperations.search(QUERY, Optional.of(QUERY_URN)).subscribe(subscriber);

        verify(cacheUniversalSearchCommand).call(apiUniversalSearchItems);
        subscriber.assertValueCount(1);
        assertThat(apiTopResults.buckets().getCollection()).isEqualTo(subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void callsCorrectApiEndpoint() throws Exception {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), any(TypeToken.class))).thenReturn(Observable.empty());

        topResultsOperations.search(QUERY, Optional.of(QUERY_URN)).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argumentCaptor.capture(), eq(TYPE_TOKEN));
        final ApiRequest apiRequest = argumentCaptor.getValue();
        assertThat(apiRequest.isPrivate()).isTrue();
        assertThat(apiRequest.getUri().toString()).isEqualTo(ApiEndpoints.SEARCH_TOP_RESULTS.path());
        assertThat(apiRequest.getMethod()).isEqualTo("GET");
        assertThat(apiRequest.getQueryParameters().get(TopResultsOperations.QUERY_PARAM).iterator().next()).isEqualTo(QUERY);
        assertThat(apiRequest.getQueryParameters().get(ApiRequest.Param.PAGE_SIZE.toString()).iterator().next()).isEqualTo("3");
        assertThat(apiRequest.getQueryParameters().get(TopResultsOperations.QUERY_URN_PARAM).iterator().next()).isEqualTo(QUERY_URN.toString());
    }

    @Test
    public void doesNotCacheWhenEmptyResponse() throws Exception {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), any(TypeToken.class))).thenReturn(Observable.empty());

        topResultsOperations.search(QUERY, Optional.of(QUERY_URN)).subscribe(subscriber);

        verify(cacheUniversalSearchCommand, never()).call(anyIterable());
        subscriber.assertNoValues();
    }
}
