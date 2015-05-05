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
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class AddCommentDialogFragment extends DialogFragment {

    private static final String EXTRA_TRACK = "track";
    private static final String EXTRA_POSITION = "position";
    private static final String EXTRA_ORIGIN_SCREEN = "origin";

    @Inject CommentsOperations commentsOperations;
    @Inject EventBus eventBus;

    public static AddCommentDialogFragment create(PropertySet track, long position, String originScreen) {
        Bundle b = new Bundle();
        b.putParcelable(EXTRA_TRACK, track);
        b.putLong(EXTRA_POSITION, position);
        b.putString(EXTRA_ORIGIN_SCREEN, originScreen);
        AddCommentDialogFragment fragment = new AddCommentDialogFragment();
        fragment.setArguments(b);
        return fragment;
    }

    public AddCommentDialogFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final PropertySet track = getArguments().getParcelable(EXTRA_TRACK);
        final String timeFormatted = ScTextUtils.formatTimestamp(getArguments().getLong(EXTRA_POSITION), TimeUnit.MILLISECONDS);

        final View dialogView = View.inflate(getActivity(), R.layout.comment_input, null);
        final EditText input = (EditText) dialogView.findViewById(R.id.comment_input);
        input.setHint(getString(R.string.comment_at, timeFormatted));

        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.comment_on, track.get(TrackProperty.TITLE)))
                .setView(dialogView)
                .setPositiveButton(R.string.post, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String commentText = input.getText().toString();
                        if (ScTextUtils.isNotBlank(commentText)) {
                            onAddComment(commentText);
                            dismiss();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    private void onAddComment(String commentText){
        final PropertySet track = getArguments().getParcelable(EXTRA_TRACK);
        final Urn trackUrn = track.get(TrackProperty.URN);
        final long position = getArguments().getLong(EXTRA_POSITION);

        final FragmentActivity activity = (FragmentActivity) getActivity();
        bindActivity(
                activity,
                commentsOperations.addComment(trackUrn, commentText, position)
        ).subscribe(new CommentAddedSubscriber(activity, track, eventBus));

        final String originScreen = getArguments().getString(EXTRA_ORIGIN_SCREEN);
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromComment(originScreen, trackUrn.getNumericId()));
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
        public void onNext(PublicApiComment comment) {
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
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerClose(UIEvent.METHOD_COMMENTS_OPEN_FROM_ADD_COMMENT));
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
