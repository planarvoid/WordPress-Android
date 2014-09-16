package com.soundcloud.android.comments;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.comments.CommentsOperations.CommentsCollection;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackUrn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class CommentsOperationsTest {

    @Mock RxHttpClient httpClient;

    private CommentsOperations operations;
    private TestObserver<List<PublicApiComment>> observer = new TestObserver<>();
    private PublicApiComment comment = ModelFixtures.create(PublicApiComment.class);

    @Before
    public void setup() {
        operations = new CommentsOperations(httpClient);
        Observable<CommentsCollection> apiComments = Observable.just(new CommentsCollection(Arrays.asList(comment)));
        when(httpClient.<CommentsCollection>fetchModels(argThat(isApiRequestTo("GET", "/tracks/123/comments")))).thenReturn(apiComments);
    }

    @Test
    public void shouldRetrieveCommentsForGivenTrack() {
        TrackUrn track = Urn.forTrack(123L);
        operations.comments(track).subscribe(observer);

        expect(observer.getOnNextEvents().get(0)).toContainExactly(comment);
    }
}