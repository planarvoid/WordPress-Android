package com.soundcloud.android.comments;

import static com.soundcloud.android.comments.CommentsOperations.COMMENTS_PAGE_SIZE;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.comments.CommentsOperations.CommentsCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.AssertableSubscriber;

import java.util.HashMap;
import java.util.Map;

public class CommentsOperationsTest extends AndroidUnitTest {

    @Mock private ApiClientRxV2 apiClientRx;
    @Mock private FeatureFlags featureFlags;

    private CommentsOperations operations;
    private ApiComment apiMobileComment = ModelFixtures.apiComment(new Urn("soundcloud:comments:12345"));
    private PublicApiComment publicApiComment = ModelFixtures.publicApiComment();
    private String nextPageUrl = "http://next-page";

    @Before
    public void setup() {
        operations = new CommentsOperations(apiClientRx, Schedulers.trampoline(), featureFlags);
    }

    @Test
    public void shouldRetrieveApiMobileCommentsForGivenTrack() {
        when(featureFlags.isEnabled(Flag.COMMENTS_ON_API_MOBILE)).thenReturn(true);
        when(apiClientRx.mappedResponse(argThat(
                isApiRequestTo("GET", "/tracks/soundcloud%3Atracks%3A123/comments")
                        .withQueryParam("limit", String.valueOf(COMMENTS_PAGE_SIZE))
                        .withQueryParam("threaded", "0")
        ), eq(CommentsOperations.TYPE_TOKEN))).thenReturn(Single.just(new ModelCollection<>(singletonList(apiMobileComment), nextPageUrl)));

        Urn track = Urn.forTrack(123L);
        operations.comments(track).test().assertValueCount(1);
    }

    @Test
    public void shouldPageApiMobileCommentsIfMorePagesAvailable() {
        when(featureFlags.isEnabled(Flag.COMMENTS_ON_API_MOBILE)).thenReturn(true);
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo(nextPageUrl)), eq(CommentsOperations.TYPE_TOKEN))).thenReturn(
                Single.just(new ModelCollection<>(singletonList(apiMobileComment), nextPageUrl)));

        AssertableSubscriber<ModelCollection<Comment>> subscriber = operations.pager()
                                                                              .page(rx.Observable.just(new ModelCollection<>(singletonList(new Comment(apiMobileComment)), nextPageUrl)))
                                                                              .test();
        operations.pager().next();

        subscriber.assertValueCount(2);
    }

    @Test
    public void shouldStopPagingApiMobileCommentsIfNoMorePagesAvailable() {
        when(featureFlags.isEnabled(Flag.COMMENTS_ON_API_MOBILE)).thenReturn(true);

        AssertableSubscriber<ModelCollection<Comment>> subscriber = operations.pager()
                                                                              .page(rx.Observable.just(new ModelCollection<>(singletonList(new Comment(apiMobileComment)), (String) null)))
                                                                              .test();
        operations.pager().next();

        subscriber.assertValueCount(1);
    }

    @Test
    public void addsApiMobileComment() {
        when(featureFlags.isEnabled(Flag.COMMENTS_ON_API_MOBILE)).thenReturn(true);
        final Map<String, Object> content = new HashMap<>(2);
        content.put("body", "some content text");
        content.put("track_time", 2001L);
        when(apiClientRx.mappedResponse(argThat(
                isApiRequestTo("POST", "/tracks/soundcloud%3Atracks%3A123/comments")
                        .withContent(content)
        ), eq(ApiComment.class))).thenReturn(Single.just(apiMobileComment));

        operations.addComment(Urn.forTrack(123L), "some content text", 2001L)
                  .test()
                  .assertValue(new Comment(apiMobileComment))
                  .assertComplete();
    }

    @Test
    public void shouldRetrievePublicApiCommentsForGivenTrack() {
        when(featureFlags.isEnabled(Flag.COMMENTS_ON_API_MOBILE)).thenReturn(false);
        when(apiClientRx.mappedResponse(argThat(
                isPublicApiRequestTo("GET", "/tracks/123/comments")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(COMMENTS_PAGE_SIZE))
        ), eq(CommentsCollection.class))).thenReturn(Single.just(new CommentsCollection(singletonList(publicApiComment), nextPageUrl)));

        operations.comments(Urn.forTrack(123L))
                  .test()
                  .assertValueCount(1);
    }

    @Test
    public void shouldPagePublicApiCommentsIfMorePagesAvailable() {
        when(featureFlags.isEnabled(Flag.COMMENTS_ON_API_MOBILE)).thenReturn(false);
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo(nextPageUrl)), eq(CommentsCollection.class))).thenReturn(
                Single.just(new CommentsCollection(singletonList(publicApiComment), nextPageUrl)));

        AssertableSubscriber<ModelCollection<Comment>> subscriber = operations.pager().page(Observable.just(new ModelCollection<>(singletonList(new Comment(publicApiComment)), nextPageUrl))).test();
        operations.pager().next();

        subscriber.assertValueCount(2);
    }

    @Test
    public void shouldStopPagingPublicApiCommentsIfNoMorePagesAvailable() {
        when(featureFlags.isEnabled(Flag.COMMENTS_ON_API_MOBILE)).thenReturn(false);

        AssertableSubscriber<ModelCollection<Comment>> subscriber = operations.pager().page(Observable.just(new ModelCollection<>(singletonList(new Comment(publicApiComment)), (String) null))).test();
        operations.pager().next();

        subscriber.assertValueCount(1);
    }

    @Test
    public void addsPublicApiComment() {
        when(featureFlags.isEnabled(Flag.COMMENTS_ON_API_MOBILE)).thenReturn(false);
        when(apiClientRx.mappedResponse(argThat(
                isPublicApiRequestTo("POST", "/tracks/123/comments")
                        .withContent(new CommentsOperations.CommentHolder("some comment text", 2001L))
        ), eq(PublicApiComment.class))).thenReturn(Single.just(publicApiComment));

        operations.addComment(Urn.forTrack(123L), "some comment text", 2001L)
                  .test()
                  .assertValue(new Comment(publicApiComment))
                  .assertComplete();
    }
}
