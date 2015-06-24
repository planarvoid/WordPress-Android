package com.soundcloud.android.comments;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.comments.CommentsOperations.COMMENTS_PAGE_SIZE;
import static com.soundcloud.android.comments.CommentsOperations.CommentsCollection;
import static com.soundcloud.android.testsupport.matchers.SoundCloudMatchers.isApiRequestTo;
import static com.soundcloud.android.testsupport.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class CommentsOperationsTest {

    @Mock private ApiClientRx apiClientRx;

    private CommentsOperations operations;
    private TestObserver<CommentsCollection> observer = new TestObserver<>();
    private PublicApiComment comment = ModelFixtures.create(PublicApiComment.class);
    private Observable<CommentsCollection> apiComments;
    private String nextPageUrl = "http://next-page";

    @Before
    public void setup() {
        operations = new CommentsOperations(apiClientRx, Schedulers.immediate());
        apiComments = Observable.just(new CommentsCollection(Arrays.asList(comment), nextPageUrl));
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

        expect(observer.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void shouldPageCommentsIfMorePagesAvailable() {
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo(nextPageUrl)), eq(CommentsCollection.class))).thenReturn(apiComments);
        operations.pager().page(apiComments).subscribe(observer);

        operations.pager().next();

        expect(observer.getOnNextEvents()).toNumber(2);
    }

    @Test
    public void shouldStopPagingIfNoMorePagesAvailable() {
        apiComments = Observable.just(new CommentsCollection(Arrays.asList(comment), null));

        operations.pager().page(apiComments).subscribe(observer);

        operations.pager().next();

        expect(observer.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void addsComment() throws Exception {
        when(apiClientRx.mappedResponse(argThat(
                isPublicApiRequestTo("POST", "/tracks/123/comments")
                        .withContent(new CommentsOperations.CommentHolder("some comment text", 2001L))
        ), eq(PublicApiComment.class))).thenReturn(Observable.just(comment));

        TestSubscriber<PublicApiComment> subscriber = new TestSubscriber<>();
        operations.addComment(Urn.forTrack(123L), "some comment text", 2001L).subscribe(subscriber);

        expect(subscriber.getOnNextEvents()).toContainExactly(comment);
        expect(subscriber.getOnCompletedEvents()).toNumber(1);
    }
}