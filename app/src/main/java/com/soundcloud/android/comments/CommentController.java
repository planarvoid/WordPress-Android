package com.soundcloud.android.comments;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.android.rx.observers.LambdaMaybeObserver;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBusV2;
import dagger.Lazy;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class CommentController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final Lazy<CommentsOperations> commentsOperationsLazy;
    private final FeedbackController feedbackController;
    private final NavigationExecutor navigationExecutor;
    private final EventBusV2 eventBus;

    private Disposable disposable = RxUtils.invalidDisposable();
    private AppCompatActivity activity;

    @Inject
    public CommentController(EventBusV2 eventBus,
                             Lazy<CommentsOperations> commentsOperationsLazy,
                             FeedbackController feedbackController,
                             NavigationExecutor navigationExecutor) {
        this.eventBus = eventBus;
        this.commentsOperationsLazy = commentsOperationsLazy;
        this.feedbackController = feedbackController;
        this.navigationExecutor = navigationExecutor;
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        super.onCreate(activity, bundle);
        this.activity = activity;
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        this.disposable.dispose();
        this.activity = null;

        super.onDestroy(activity);
    }

    public void addComment(AddCommentArguments arguments) {
        final Urn trackUrn = arguments.trackUrn();

        disposable = commentsOperationsLazy.get().addComment(trackUrn, arguments.getCommentText(), arguments.getPosition())
                                           .observeOn(AndroidSchedulers.mainThread())
                                           .subscribeWith(new CommentAddedObserver(trackUrn));

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromComment(EntityMetadata.from(arguments.creatorName(), arguments.creatorUrn(), arguments.trackTitle(), arguments.trackUrn())));
    }

    private class CommentAddedObserver extends DefaultSingleObserver<Comment> {

        private final Urn trackUrn;

        CommentAddedObserver(Urn trackUrn) {
            this.trackUrn = trackUrn;
        }

        @Override
        public void onSuccess(Comment unused) {
            super.onSuccess(unused);
            final Feedback feedback = Feedback.create(R.string.comment_posted, R.string.btn_view, v -> {
                subscribeToCollapsedEvent(activity);
                eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayerAutomatically());
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
                    .filter(PlayerUIEvent.PLAYER_IS_COLLAPSED_V2)
                    .firstElement()
                    .subscribeWith(LambdaMaybeObserver.onNext(playerUIEvent -> navigationExecutor.openTrackComments(context, trackUrn)));
        }
    }
}
