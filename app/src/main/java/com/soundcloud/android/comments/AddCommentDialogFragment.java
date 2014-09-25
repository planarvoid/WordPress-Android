package com.soundcloud.android.comments;

import static rx.android.observables.AndroidObservable.bindActivity;

import com.cocosw.undobar.UndoBarController;
import com.cocosw.undobar.UndoBarController.UndoBar;
import com.cocosw.undobar.UndoBarStyle;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import eu.inmite.android.lib.dialogs.BaseDialogFragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class AddCommentDialogFragment extends BaseDialogFragment {

    private static final String KEY_TRACK = "TRACK";
    private static final String KEY_POSITION = "POSITION";
    private static final String KEY_ORIGIN_SCREEN = "ORIGIN_SCREEN";

    @Inject CommentsOperations commentsOperations;
    @Inject EventBus eventBus;

    public static AddCommentDialogFragment create(PropertySet track, PlaybackProgress lastProgress, String originScreen) {
        Bundle b = new Bundle();
        b.putParcelable(KEY_TRACK, track);
        b.putLong(KEY_POSITION, lastProgress.getPosition());
        b.putString(KEY_ORIGIN_SCREEN, originScreen);
        AddCommentDialogFragment fragment = new AddCommentDialogFragment();
        fragment.setArguments(b);
        return fragment;
    }

    public AddCommentDialogFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected Builder build(Builder builder) {
        final PropertySet track = getArguments().getParcelable(KEY_TRACK);
        final View dialogView = View.inflate(getActivity(), R.layout.dialog_comment_at, null);
        final EditText input = (EditText) dialogView.findViewById(android.R.id.edit);
        final String timeFormatted = ScTextUtils.formatTimestamp(getArguments().getLong(KEY_POSITION), TimeUnit.MILLISECONDS);
        input.setHint(getString(R.string.comment_at, timeFormatted));

        builder.setView(dialogView);
        builder.setTitle(getString(R.string.comment_on, track.get(TrackProperty.TITLE)));
        builder.setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        builder.setPositiveButton(R.string.post, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String commentText = input.getText().toString();
                if (ScTextUtils.isNotBlank(commentText)) {
                    onAddComment(commentText);
                    dismiss();
                }
            }
        });
        return builder;
    }

    private void onAddComment(String commentText){
        final PropertySet track = getArguments().getParcelable(KEY_TRACK);
        final Urn trackUrn = track.get(TrackProperty.URN);
        final long position = getArguments().getLong(KEY_POSITION);

        final FragmentActivity activity = getActivity();
        bindActivity(
                activity,
                commentsOperations.addComment(trackUrn, commentText, position)
        ).subscribe(new CommentAddedSubscriber(activity, track, eventBus));

        final String originScreen = getArguments().getString(KEY_ORIGIN_SCREEN);
        eventBus.publish(EventQueue.UI_TRACKING, UIEvent.fromComment(originScreen, trackUrn.getNumericId()));
    }

    @VisibleForTesting
    static final class CommentAddedSubscriber extends DefaultSubscriber<PublicApiComment> implements UndoBarController.UndoListener {

        private final Activity activity;
        private final PropertySet track;
        private final EventBus eventBus;

        CommentAddedSubscriber(Activity activity, PropertySet track, EventBus eventBus) {
            this.activity = activity;
            this.track = track;
            this.eventBus = eventBus;
        }

        @Override
        public void onNext(PublicApiComment playlist) {
            new UndoBar(activity)
                    .message(R.string.your_comment_has_been_posted)
                    .style(createViewCommentBarStyle())
                    .listener(this)
                    .show();
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            Toast.makeText(activity, activity.getString(R.string.comment_could_not_be_created_at_this_time), Toast.LENGTH_SHORT).show();
        }

        private UndoBarStyle createViewCommentBarStyle() {
            return new UndoBarStyle(R.drawable.undobar_button, R.string.view)
                    .setAnim(AnimationUtils.loadAnimation(activity, android.R.anim.fade_in),
                            AnimationUtils.loadAnimation(activity, android.R.anim.fade_out));
        }

        @Override
        public void onUndo(Parcelable parcelable) {
            subscribeToCollapsedEvent(activity);
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());
            eventBus.publish(EventQueue.UI_TRACKING, UIEvent.fromPlayerClose(UIEvent.METHOD_COMMENTS_OPEN_FROM_ADD_COMMENT));
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
                    context.startActivity(new Intent(context, TrackCommentsActivity.class)
                            .putExtra(TrackCommentsActivity.EXTRA_COMMENTED_TRACK, track));
                }
            };
        }
    }

}
