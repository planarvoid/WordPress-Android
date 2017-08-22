package com.soundcloud.android.comments;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.IOException;

public class CommentControllerTest extends AndroidUnitTest {

    private static final long POSITION = 123L;
    private static final String COMMENT = "comment";
    private static final String ORIGIN = "origin";

    @Mock private CommentsOperations commentOperations;
    @Mock private AppCompatActivity activity;
    @Mock private View player;
    @Mock private FeedbackController feedbackController;
    @Mock private NavigationExecutor navigationExecutor;
    @Captor private ArgumentCaptor<Feedback> feedbackArgumentCaptor;

    private final TestEventBus eventBus = new TestEventBus();
    private TrackItem track;
    private Comment comment;

    private CommentController controller;


    @Before
    public void setUp() throws Exception {
        track = PlayableFixtures.fromApiTrack();
        comment = new Comment(ModelFixtures.apiComment(new Urn("soundcloud:comments:123")));

        controller = new CommentController(eventBus, InjectionSupport.lazyOf(commentOperations), feedbackController, navigationExecutor);
        controller.onCreate(activity, null);
    }

    @Test
    public void showsSuccessFeedbackAfterPost() {
        when(commentOperations.addComment(track.getUrn(), COMMENT, POSITION)).thenReturn(Observable.just(comment));

        controller.addComment(AddCommentArguments.create(track.title(), track.getUrn(), track.creatorName(), track.creatorUrn(), POSITION, COMMENT, ORIGIN));

        verify(feedbackController).showFeedback(feedbackArgumentCaptor.capture());
        assertThat(feedbackArgumentCaptor.getValue().getMessage()).isEqualTo(R.string.comment_posted);
    }

    @Test
    public void showsFailureFeedbackAfterPost() {
        when(commentOperations.addComment(track.getUrn(), COMMENT, POSITION))
                .thenReturn(Observable.error(new IOException()));

        controller.addComment(AddCommentArguments.create(track.title(), track.getUrn(), track.creatorName(), track.creatorUrn(), POSITION, COMMENT, ORIGIN));

        verify(feedbackController).showFeedback(feedbackArgumentCaptor.capture());
        assertThat(feedbackArgumentCaptor.getValue().getMessage()).isEqualTo(R.string.comment_error);
    }

    @Test
    public void unsubscribesInOnDestroy() {
        final PublishSubject<Comment> subject = PublishSubject.create();
        when(commentOperations.addComment(track.getUrn(), COMMENT, POSITION))
                .thenReturn(subject);

        controller.addComment(AddCommentArguments.create(track.title(), track.getUrn(), track.creatorName(), track.creatorUrn(), POSITION, COMMENT, ORIGIN));

        assertThat(subject.hasObservers()).isTrue();

        controller.onDestroy(activity);

        assertThat(subject.hasObservers()).isFalse();
    }
}
