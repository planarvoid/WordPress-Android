package com.soundcloud.android.comments;


import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.strings.Strings;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class AddCommentDialogFragment extends DialogFragment {

    private static final String EXTRA_TRACK = "track";
    private static final String EXTRA_POSITION = "position";
    private static final String EXTRA_ORIGIN_SCREEN = "origin";

    public static AddCommentDialogFragment create(TrackItem track, long position, String originScreen) {
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
        final TrackItem track = getArguments().getParcelable(EXTRA_TRACK);
        final long position = getArguments().getLong(EXTRA_POSITION);
        final String timeFormatted = ScTextUtils.formatTimestamp(position, TimeUnit.MILLISECONDS);

        final View dialogView = View.inflate(getActivity(), R.layout.comment_input, null);
        final TextView title = (TextView) dialogView.findViewById(R.id.custom_dialog_title);
        final EditText input = (EditText) dialogView.findViewById(R.id.comment_input);

        title.setText(getString(R.string.comment_on_tracktitle, track.getTitle()));
        input.setHint(getString(R.string.comment_at_time, timeFormatted));

        return new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setPositiveButton(R.string.btn_post, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String commentText = input.getText().toString();
                        if (Strings.isNotBlank(commentText)) {
                            addComment(commentText, track, position);
                            dismiss();
                        }
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
    }

    public void addComment(String commentText, TrackItem track, long position) {
        final String originScreen = getArguments().getString(EXTRA_ORIGIN_SCREEN);
        final PlayerActivity activity = (PlayerActivity) getActivity();
        activity.addComment(AddCommentArguments.create(track, position, commentText, originScreen));
    }
}
