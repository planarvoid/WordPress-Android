package com.soundcloud.android.comments;

import static rx.android.observables.AndroidObservable.bindActivity;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ScTextUtils;
import eu.inmite.android.lib.dialogs.BaseDialogFragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class AddCommentDialogFragment extends BaseDialogFragment {

    private static final String KEY_TRACK_URN = "TRACK_URN";
    private static final String KEY_TRACK_TITLE = "TRACK_TITLE";
    private static final String KEY_POSITION = "POSITION";
    private static final String KEY_ORIGIN_SCREEN = "ORIGIN_SCREEN";

    @Inject CommentsOperations commentsOperations;
    @Inject EventBus eventBus;

    public static AddCommentDialogFragment create(Urn urn, String title, PlaybackProgress lastProgress, String originScreen) {
        Bundle b = new Bundle();
        b.putParcelable(KEY_TRACK_URN, urn);
        b.putString(KEY_TRACK_TITLE, title);
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

        final View dialogView = View.inflate(getActivity(), R.layout.dialog_comment_at, null);
        final EditText input = (EditText) dialogView.findViewById(android.R.id.edit);
        final String timeFormatted = ScTextUtils.formatTimestamp(getArguments().getLong(KEY_POSITION), TimeUnit.MILLISECONDS);
        input.setHint(getString(R.string.comment_at, timeFormatted));

        builder.setView(dialogView);
        builder.setTitle(getString(R.string.comment_on, getArguments().getString(KEY_TRACK_TITLE)));
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
        final Urn trackUrn = getArguments().getParcelable(KEY_TRACK_URN);
        final long position = getArguments().getLong(KEY_POSITION);

        final FragmentActivity activity = getActivity();
        bindActivity(
                activity,
                commentsOperations.addComment(trackUrn, commentText, position)
        ).subscribe(new CommentAddedSubscriber(activity));

        final String originScreen = getArguments().getString(KEY_ORIGIN_SCREEN);
        eventBus.publish(EventQueue.UI_TRACKING, UIEvent.fromComment(originScreen, trackUrn.getNumericId()));
    }

    private static final class CommentAddedSubscriber extends DefaultSubscriber<PublicApiComment> {

        private final Context context;

        private CommentAddedSubscriber(Context context) {
            this.context = context;
        }

        @Override
        public void onNext(PublicApiComment playlist) {
            Toast.makeText(context, R.string.your_comment_has_been_posted, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            Toast.makeText(context, context.getString(R.string.comment_could_not_be_created_at_this_time), Toast.LENGTH_SHORT).show();
        }
    }

}
