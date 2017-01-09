package com.soundcloud.android.comments;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import javax.inject.Inject;

public class CommentController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final Lazy<CommentsOperations> commentsOperationsLazy;
    private final FeedbackController feedbackController;
    private final Navigator navigator;
    private final EventBus eventBus;

    private Subscription subscription = RxUtils.invalidSubscription();
    private AppCompatActivity activity;

    @Inject
    public CommentController(EventBus eventBus,
                             Lazy<CommentsOperations> commentsOperationsLazy,
                             FeedbackController feedbackController,
                             Navigator navigator) {
        this.eventBus = eventBus;
        this.commentsOperationsLazy = commentsOperationsLazy;
        this.feedbackController = feedbackController;
        this.navigator = navigator;
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        super.onCreate(activity, bundle);
        this.activity = activity;
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        this.subscription.unsubscribe();
        this.activity = null;

        super.onDestroy(activity);
    }

    public void addComment(AddCommentArguments arguments) {
        final Urn trackUrn = arguments.getTrack().getUrn();

        subscription = commentsOperationsLazy.get().addComment(trackUrn, arguments.getCommentText(), arguments.getPosition())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CommentAddedSubscriber(trackUrn));

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromComment(getEventContextMetadata(arguments.getOriginScreen()),
                EntityMetadata.from(arguments.getTrack())));
    }

    private EventContextMetadata getEventContextMetadata(String originScreen) {
        return EventContextMetadata.builder().contextScreen(originScreen).build();
    }

    private class CommentAddedSubscriber extends DefaultSubscriber<PublicApiComment> {

        private final Urn trackUrn;

        CommentAddedSubscriber(Urn trackUrn) {
            this.trackUrn = trackUrn;
        }

        @Override
        public void onNext(PublicApiComment comment) {
            final Feedback feedback = Feedback.create(R.string.comment_posted,
                                                      R.string.btn_view, v -> {
                                                          subscribeToCollapsedEvent(activity);
                                                          eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());
                                                      });
            feedbackController.showFeedback(feedback);

        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            final Feedback feedback = Feedback.create(R.string.comment_error, Feedback.LENGTH_LONG);
            feedbackController.showFeedback(feedback);
        }

        private void subscribeToCollapsedEvent(Context context) {
            eventBus.queue(EventQueue.PLAYER_UI)
                    .first(PlayerUIEvent.PLAYER_IS_COLLAPSED)
                    .subscribe(goToCommentsPage(context));
        }

        private DefaultSubscriber<PlayerUIEvent> goToCommentsPage(final Context context) {
            return new DefaultSubscriber<PlayerUIEvent>() {
                @Override
                public void onNext(PlayerUIEvent args) {
                    navigator.openTrackComments(context, trackUrn);
                }
            };
        }
    }
}
