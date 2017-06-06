package com.soundcloud.android.search.topresults;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.CacheUniversalSearchCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TopResultsOperationsTest extends AndroidUnitTest {

    private static final String QUERY = "query";
    private static final Urn QUERY_URN = new Urn("soundcloud:query_urn:123");
    private static final SearchParams SEARCH_PARAMS = SearchParams.create(QUERY, QUERY, Optional.of(QUERY_URN), Optional.of(2));
    private static final Urn TRACKS_BUCKET_URN = new Urn("soundcloud:search-buckets:freetiertracks");
    private static final Urn USERS_BUCKET_URN = new Urn("soundcloud:search-buckets:users");
    private static final Urn UNKNOWN_BUCKET_URN = new Urn("soundcloud:search-buckets:unknown");
    private static final ApiUniversalSearchItem UNIVERSAL_TRACK_ITEM = new ApiUniversalSearchItem(null, null, ModelFixtures.create(ApiTrack.class));
    private static final ApiUniversalSearchItem UNIVERSAL_USER_ITEM = new ApiUniversalSearchItem(ModelFixtures.create(ApiUser.class), null, null);
    private static final TypeToken<ApiTopResults> TYPE_TOKEN = new TypeToken<ApiTopResults>() {
    };

    private final rx.subjects.BehaviorSubject<LikedStatuses> likesStatuses = rx.subjects.BehaviorSubject.create();
    private final BehaviorSubject<FollowingStatuses> followingStatuses = BehaviorSubject.create();
    private final rx.subjects.BehaviorSubject<Urn> nowPlaying = rx.subjects.BehaviorSubject.create();

    @Mock private CacheUniversalSearchCommand cacheUniversalSearchCommand;
    @Mock private ApiClientRxV2 apiClientRx;
    @Mock private LikesStateProvider likesStateProvider;
    @Mock private FollowingStateProvider followingStateProvider;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Captor private ArgumentCaptor<ApiRequest> argumentCaptor;

    private TopResultsOperations topResultsOperations;
    private Scheduler scheduler = Schedulers.trampoline();

    @Before
    public void setUp() throws Exception {
        topResultsOperations = new TopResultsOperations(apiClientRx,
                                                        scheduler,
                                                        cacheUniversalSearchCommand,
                                                        likesStateProvider,
                                                        followingStateProvider,
                                                        playSessionStateProvider,
                                                        ModelFixtures.entityItemCreator());
        when(likesStateProvider.likedStatuses()).thenReturn(likesStatuses);
        when(followingStateProvider.followingStatuses()).thenReturn(followingStatuses);
        when(playSessionStateProvider.nowPlayingUrn()).thenReturn(nowPlaying);
        likesStatuses.onNext(LikedStatuses.create(Collections.emptySet()));
        followingStatuses.onNext(FollowingStatuses.create(Collections.emptySet()));
        nowPlaying.onNext(Urn.NOT_SET);
    }

    @Test
    public void performSearchAndCacheResults() throws Exception {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = newArrayList(UNIVERSAL_TRACK_ITEM);
        final ApiTopResultsBucket apiTopResultsBucket = ApiTopResultsBucket.create(TRACKS_BUCKET_URN, 5, new ModelCollection<>(apiUniversalSearchItems, Collections.emptyMap(), QUERY_URN));
        final ModelCollection<ApiTopResultsBucket> buckets = new ModelCollection<>(newArrayList(apiTopResultsBucket), Collections.emptyMap(), QUERY_URN);
        final ApiTopResults apiTopResults = ApiTopResults.create(10, buckets);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(TYPE_TOKEN))).thenReturn(Single.just(apiTopResults));

        final TestObserver<SearchResult> subscriber = topResultsOperations.search(SEARCH_PARAMS).test();

        verify(cacheUniversalSearchCommand).call(apiUniversalSearchItems);
        final List<SearchResult> searchResults = subscriber.assertValueCount(2)
                                                           .assertValueAt(0, result -> result.kind() == SearchResult.Kind.LOADING)
                                                           .assertValueAt(1, result -> result.kind() == SearchResult.Kind.DATA)
                                                           .values();
        assertCorrectData(((SearchResult.Data) searchResults.get(1)).data(), apiTopResults);
    }

    @Test
    public void emitUpdatedResultsOnLike() throws Exception {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = newArrayList(UNIVERSAL_TRACK_ITEM);
        final ApiTopResultsBucket apiTopResultsBucket = ApiTopResultsBucket.create(TRACKS_BUCKET_URN, 5, new ModelCollection<>(apiUniversalSearchItems, Collections.emptyMap(), QUERY_URN));
        final ModelCollection<ApiTopResultsBucket> buckets = new ModelCollection<>(newArrayList(apiTopResultsBucket), Collections.emptyMap(), QUERY_URN);
        final ApiTopResults apiTopResults = ApiTopResults.create(10, buckets);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(TYPE_TOKEN))).thenReturn(Single.just(apiTopResults));

        final TestObserver<SearchResult> subscriber = topResultsOperations.search(SEARCH_PARAMS).test();

        likesStatuses.onNext(LikedStatuses.create(Sets.newHashSet(UNIVERSAL_TRACK_ITEM.track().get().getUrn())));

        verify(cacheUniversalSearchCommand).call(apiUniversalSearchItems);
        final List<SearchResult> searchResults = subscriber.assertValueCount(3)
                                                           .assertValueAt(0, result -> result.kind() == SearchResult.Kind.LOADING)
                                                           .assertValueAt(1, result -> result.kind() == SearchResult.Kind.DATA)
                                                           .assertValueAt(2, result -> result.kind() == SearchResult.Kind.DATA)
                                                           .values();

        final TopResults notLikedResult = ((SearchResult.Data) searchResults.get(1)).data();
        assertCorrectData(notLikedResult, apiTopResults);
        assertThat(notLikedResult.buckets().get(0).items().get(0).trackItem().get().isUserLike()).isFalse();

        final TopResults likedResult = ((SearchResult.Data) searchResults.get(2)).data();
        assertCorrectData(likedResult, apiTopResults);
        assertThat(likedResult.buckets().get(0).items().get(0).trackItem().get().isUserLike()).isTrue();

    }

    @Test
    public void emitUpdatedResultsOnNowPlayingChange() throws Exception {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = newArrayList(UNIVERSAL_TRACK_ITEM);
        final ApiTopResultsBucket apiTopResultsBucket = ApiTopResultsBucket.create(TRACKS_BUCKET_URN, 5, new ModelCollection<>(apiUniversalSearchItems, Collections.emptyMap(), QUERY_URN));
        final ModelCollection<ApiTopResultsBucket> buckets = new ModelCollection<>(newArrayList(apiTopResultsBucket), Collections.emptyMap(), QUERY_URN);
        final ApiTopResults apiTopResults = ApiTopResults.create(10, buckets);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(TYPE_TOKEN))).thenReturn(Single.just(apiTopResults));

        final TestObserver<SearchResult> subscriber = topResultsOperations.search(SEARCH_PARAMS).test();

        nowPlaying.onNext(UNIVERSAL_TRACK_ITEM.track().get().getUrn());

        verify(cacheUniversalSearchCommand).call(apiUniversalSearchItems);
        final List<SearchResult> searchResults = subscriber.assertValueCount(3)
                                                           .assertValueAt(0, result -> result.kind() == SearchResult.Kind.LOADING)
                                                           .assertValueAt(1, result -> result.kind() == SearchResult.Kind.DATA)
                                                           .assertValueAt(2, result -> result.kind() == SearchResult.Kind.DATA)
                                                           .values();

        final TopResults notLikedResult = ((SearchResult.Data) searchResults.get(1)).data();
        assertCorrectData(notLikedResult, apiTopResults);
        assertThat(notLikedResult.buckets().get(0).items().get(0).trackItem().get().isPlaying()).isFalse();

        final TopResults likedResult = ((SearchResult.Data) searchResults.get(2)).data();
        assertCorrectData(likedResult, apiTopResults);
        assertThat(likedResult.buckets().get(0).items().get(0).trackItem().get().isPlaying()).isTrue();

    }

    @Test
    public void emitUpdatedResultsOnFollow() throws Exception {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = newArrayList(UNIVERSAL_USER_ITEM);
        final ApiTopResultsBucket apiTopResultsBucket = ApiTopResultsBucket.create(USERS_BUCKET_URN, 5, new ModelCollection<>(apiUniversalSearchItems, Collections.emptyMap(), QUERY_URN));
        final ModelCollection<ApiTopResultsBucket> buckets = new ModelCollection<>(newArrayList(apiTopResultsBucket), Collections.emptyMap(), QUERY_URN);
        final ApiTopResults apiTopResults = ApiTopResults.create(10, buckets);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(TYPE_TOKEN))).thenReturn(Single.just(apiTopResults));

        final TestObserver<SearchResult> subscriber = topResultsOperations.search(SEARCH_PARAMS).test();

        followingStatuses.onNext(FollowingStatuses.create(Sets.newHashSet(UNIVERSAL_USER_ITEM.user().get().getUrn())));

        verify(cacheUniversalSearchCommand).call(apiUniversalSearchItems);
        final List<SearchResult> searchResults = subscriber.assertValueCount(3)
                                                           .assertValueAt(0, result -> result.kind() == SearchResult.Kind.LOADING)
                                                           .assertValueAt(1, result -> result.kind() == SearchResult.Kind.DATA)
                                                           .assertValueAt(2, result -> result.kind() == SearchResult.Kind.DATA)
                                                           .values();

        final TopResults notLikedResult = ((SearchResult.Data) searchResults.get(1)).data();
        assertCorrectData(notLikedResult, apiTopResults);
        assertThat(notLikedResult.buckets().get(0).items().get(0).userItem().get().isFollowedByMe()).isFalse();

        final TopResults likedResult = ((SearchResult.Data) searchResults.get(2)).data();
        assertCorrectData(likedResult, apiTopResults);
        assertThat(likedResult.buckets().get(0).items().get(0).userItem().get().isFollowedByMe()).isTrue();
    }

    private void assertCorrectData(TopResults topResults, ApiTopResults apiTopResults) {
        assertThat(topResults.queryUrn()).isEqualTo(apiTopResults.buckets().getQueryUrn());
        assertThat(topResults.buckets()).hasSameSizeAs(apiTopResults.buckets());
        assertThat(topResults.queryUrn()).isEqualTo(apiTopResults.buckets().getQueryUrn());
        final List<TopResults.Bucket> buckets = topResults.buckets();
        final List<ApiTopResultsBucket> apiBuckets = apiTopResults.buckets().getCollection();
        for (int i = 0; i < buckets.size(); i++) {
            final TopResults.Bucket bucket = buckets.get(i);
            final ApiTopResultsBucket apiBucket = apiBuckets.get(i);
            assertThat(bucket.totalResults()).isEqualTo(apiBucket.totalResults());
            assertThat(bucket.items()).hasSameSizeAs(apiBucket.collection());
            for (int j = 0; j < bucket.items().size(); j++) {
                final DomainSearchItem domainSearchItem = bucket.items().get(j);
                final ApiUniversalSearchItem apiUniversalSearchItem = apiBucket.collection().getCollection().get(j);
                assertThat(domainSearchItem.playlistItem().isPresent()).isEqualTo(apiUniversalSearchItem.playlist().isPresent());
                assertThat(domainSearchItem.userItem().isPresent()).isEqualTo(apiUniversalSearchItem.user().isPresent());
                assertThat(domainSearchItem.trackItem().isPresent()).isEqualTo(apiUniversalSearchItem.track().isPresent());
            }
        }
    }

    @Test
    public void filtersOutUnknownBucket() throws Exception {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = newArrayList(UNIVERSAL_TRACK_ITEM);
        final ApiTopResultsBucket apiTopResultsBucket = ApiTopResultsBucket.create(UNKNOWN_BUCKET_URN, 5, new ModelCollection<>(apiUniversalSearchItems, Collections.emptyMap(), QUERY_URN));
        final ModelCollection<ApiTopResultsBucket> buckets = new ModelCollection<>(newArrayList(apiTopResultsBucket), Collections.emptyMap(), QUERY_URN);
        final ApiTopResults apiTopResults = ApiTopResults.create(10, buckets);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(TYPE_TOKEN))).thenReturn(Single.just(apiTopResults));

        final TestObserver<SearchResult> subscriber = topResultsOperations.search(SEARCH_PARAMS).test();

        verify(cacheUniversalSearchCommand).call(apiUniversalSearchItems);
        subscriber.assertValueCount(2)
                  .assertValueAt(0, result -> result.kind() == SearchResult.Kind.LOADING)
                  .assertValueAt(1, result -> result.kind() == SearchResult.Kind.DATA)
                  .assertValueAt(1, result -> ((SearchResult.Data) result).data().buckets().size() == 0);

    }

    @Test
    public void callsCorrectApiEndpoint() throws Exception {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), any(TypeToken.class))).thenReturn(Single.error(new IOException()));

        topResultsOperations.search(SEARCH_PARAMS).test();

        verify(apiClientRx).mappedResponse(argumentCaptor.capture(), eq(TYPE_TOKEN));
        final ApiRequest apiRequest = argumentCaptor.getValue();
        assertThat(apiRequest.isPrivate()).isTrue();
        assertThat(apiRequest.getUri().toString()).isEqualTo(ApiEndpoints.SEARCH_TOP_RESULTS.path());
        assertThat(apiRequest.getMethod()).isEqualTo("GET");
        assertThat(apiRequest.getQueryParameters().get(TopResultsOperations.QUERY_PARAM).iterator().next()).isEqualTo(QUERY);
        assertThat(apiRequest.getQueryParameters().get(ApiRequest.Param.PAGE_SIZE.toString()).iterator().next()).isEqualTo("2");
        assertThat(apiRequest.getQueryParameters().get(TopResultsOperations.QUERY_URN_PARAM).iterator().next()).isEqualTo(QUERY_URN.toString());
    }

    @Test
    public void doesNotCacheWhenEmptyResponse() throws Exception {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), any(TypeToken.class))).thenReturn(Single.error(new IOException()));

        final TestObserver<SearchResult> subscriber = topResultsOperations.search(SEARCH_PARAMS).test();

        verify(cacheUniversalSearchCommand, never()).call(anyIterable());
        subscriber.assertValueCount(2)
                  .assertValueAt(0, result -> result.kind() == SearchResult.Kind.LOADING)
                  .assertValueAt(1, result -> result.kind() == SearchResult.Kind.ERROR);
    }
}
