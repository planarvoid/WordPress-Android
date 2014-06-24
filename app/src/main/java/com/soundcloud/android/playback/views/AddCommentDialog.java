
package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.associations.AddCommentTask;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import eu.inmite.android.lib.dialogs.BaseDialogFragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class AddCommentDialog extends BaseDialogFragment {

    private static final String EXTRA_COMMENT = "comment";
    private static final String EXTRA_ORIGIN_SCREEN = "origin_screen";

    public static AddCommentDialog from(Comment comment, String originScreen) {
        Bundle b = new Bundle();
        b.putParcelable(EXTRA_COMMENT,comment);
        b.putString(EXTRA_ORIGIN_SCREEN, originScreen);
        AddCommentDialog addCommentDialog = new AddCommentDialog();
        addCommentDialog.setArguments(b);
        return addCommentDialog;
    }

    @Override
    protected Builder build(Builder initialBuilder) {
        final Comment comment = getArguments().getParcelable(EXTRA_COMMENT);
        final EventBus eventBus = ((SoundCloudApplication) getActivity().getApplication()).getEventBus();

        final View dialogView = View.inflate(getActivity(), R.layout.add_new_comment_dialog_view, null);

        final EditText input = (EditText) dialogView.findViewById(R.id.comment_input);
        configureHint(comment, input);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE && applyTextAndUpload(comment, input.getText().toString())){
                    handleCommentAdded(comment, eventBus);
                    return true;
                }
                return false;
            }
        });
        initialBuilder.setView(dialogView);
        initialBuilder.setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        initialBuilder.setPositiveButton(R.string.done, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (applyTextAndUpload(comment, input.getText().toString())){
                    handleCommentAdded(comment, eventBus);
                }
            }
        });
        return initialBuilder;
    }

    private void handleCommentAdded(Comment comment, EventBus eventBus) {
        final String screenTag = getArguments().getString(EXTRA_ORIGIN_SCREEN);
        eventBus.publish(EventQueue.UI, UIEvent.fromComment(screenTag, comment.track_id));
        dismiss();
    }

    private void configureHint(Comment comment, EditText input) {
        if (comment.reply_to_id > 0) {
            input.setHint(getString(R.string.comment_hint_reply,
                    comment.reply_to_username,
                    ScTextUtils.formatTimestamp(comment.timestamp, TimeUnit.MILLISECONDS)));
        } else {
            input.setHint(comment.timestamp == -1 ?
                    getString(R.string.comment_hint_untimed) :
                    getString(R.string.comment_hint_timed, ScTextUtils.formatTimestamp(comment.timestamp, TimeUnit.MILLISECONDS)));
        }
    }

    private boolean applyTextAndUpload(final Comment comment, String commentBody) {
        if (!IOUtils.isConnected(getActivity())){
            Toast.makeText(getActivity(), R.string.add_comment_no_connection,Toast.LENGTH_LONG).show();
            return false;
        }

        if (!TextUtils.isEmpty(commentBody))  {
            comment.body = commentBody;
            final Track track = SoundCloudApplication.sModelManager.getTrack(comment.track_id);
            if (track != null) {
                if (track.comments == null) track.comments = new ArrayList<Comment>();
                // add dummy comment
                track.comments.add(comment);
                track.comment_count = Math.max(1, track.comment_count + 1); //take care of -1
            }

            getActivity().sendBroadcast(new Intent(Playable.COMMENT_ADDED).putExtra(Comment.EXTRA, comment));
            // TODO, port to RX
            new AddCommentTask(getActivity().getApplicationContext()).execute(comment);

            return true;
        } else {
            return false;
        }
    }
}
