package com.soundcloud.android.comments;

import static com.soundcloud.android.comments.CommentsOperations.COMMENTS_PAGE_SIZE;
import static com.soundcloud.android.comments.CommentsOperations.CommentsCollection;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class CommentsOperationsTest extends AndroidUnitTest {

    @Mock private ApiClientRx apiClientRx;

    private CommentsOperations operations;
    private TestObserver<CommentsCollection> observer = new TestObserver<>();
    private PublicApiComment comment = ModelFixtures.create(PublicApiComment.class);
    private Observable<CommentsCollection> apiComments;
    private String nextPageUrl = "http://next-page";

    @Before
    public void setup() {
        operations = new CommentsOperations(apiClientRx, Schedulers.immediate());
        apiComments = Observable.just(new CommentsCollection(singletonList(comment), nextPageUrl));
        when(apiClientRx.mappedResponse(argThat(
                isPublicApiRequestTo("GET", "/tracks/123/comments")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(COMMENTS_PAGE_SIZE))
        ), eq(CommentsCollection.class))).thenReturn(apiComments);
    }

    @Test
    public void shouldRetrieveCommentsForGivenTrack() {
        Urn track = Urn.forTrack(123L);
        operations.comments(track).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void shouldPageCommentsIfMorePagesAvailable() {
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo(nextPageUrl)), eq(CommentsCollection.class))).thenReturn(
                apiComments);
        operations.pager().page(apiComments).subscribe(observer);

        operations.pager().next();

        assertThat(observer.getOnNextEvents()).hasSize(2);
    }

    @Test
    public void shouldStopPagingIfNoMorePagesAvailable() {
        apiComments = Observable.just(new CommentsCollection(singletonList(comment), null));

        operations.pager().page(apiComments).subscribe(observer);

        operations.pager().next();

        assertThat(observer.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void addsComment() {
        when(apiClientRx.mappedResponse(argThat(
                isPublicApiRequestTo("POST", "/tracks/123/comments")
                        .withContent(new CommentsOperations.CommentHolder("some comment text", 2001L))
        ), eq(PublicApiComment.class))).thenReturn(Observable.just(comment));

        TestSubscriber<PublicApiComment> subscriber = new TestSubscriber<>();
        operations.addComment(Urn.forTrack(123L), "some comment text", 2001L).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).containsExactly(comment);
        assertThat(subscriber.getOnCompletedEvents()).hasSize(1);
    }
}
