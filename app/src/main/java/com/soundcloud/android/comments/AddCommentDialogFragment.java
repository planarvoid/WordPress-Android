package com.soundcloud.android.comments;


import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.strings.Strings;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class AddCommentDialogFragment extends DialogFragment {

    private static final String EXTRA_TRACK_URN = "track_urn";
    private static final String EXTRA_TRACK_TITLE = "track_title";
    private static final String EXTRA_CREATOR_URN = "creator_urn";
    private static final String EXTRA_CREATOR_NAME = "creator_name";
    private static final String EXTRA_POSITION = "position";
    private static final String EXTRA_ORIGIN_SCREEN = "origin";

    @Inject
    TrackRepository trackRepository;

    public static AddCommentDialogFragment create(TrackItem track, long position, String originScreen) {
        Bundle b = new Bundle();
        b.putParcelable(EXTRA_TRACK_URN, track.getUrn());
        b.putString(EXTRA_TRACK_TITLE, track.title());
        b.putParcelable(EXTRA_CREATOR_URN, track.creatorUrn());
        b.putString(EXTRA_CREATOR_NAME, track.creatorName());
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
        final Urn trackUrn = getArguments().getParcelable(EXTRA_TRACK_URN);
        final String trackTitle = getArguments().getString(EXTRA_TRACK_TITLE);
        final Urn creatorUrn = getArguments().getParcelable(EXTRA_CREATOR_URN);
        final String creatorName = getArguments().getString(EXTRA_CREATOR_NAME);
        final long position = getArguments().getLong(EXTRA_POSITION);
        final String timeFormatted = ScTextUtils.formatTimestamp(position, TimeUnit.MILLISECONDS);

        final View dialogView = View.inflate(getActivity(), R.layout.comment_input, null);
        final TextView title = (TextView) dialogView.findViewById(R.id.custom_dialog_title);
        final EditText input = (EditText) dialogView.findViewById(R.id.comment_input);

        title.setText(getString(R.string.comment_on_tracktitle, trackTitle));
        input.setHint(getString(R.string.comment_at_time, timeFormatted));

        return new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setPositiveButton(R.string.btn_post, (dialog, which) -> {
                    final String commentText = input.getText().toString();
                    if (Strings.isNotBlank(commentText)) {
                        addComment(commentText, trackUrn, trackTitle, creatorUrn, creatorName, position);
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
    }

    public void addComment(String commentText, Urn trackUrn, String trackTitle, Urn creatorUrn, String creatorName, long position) {
        final String originScreen = getArguments().getString(EXTRA_ORIGIN_SCREEN);
        final PlayerActivity activity = (PlayerActivity) getActivity();
        activity.addComment(AddCommentArguments.create(trackTitle, trackUrn, creatorName, creatorUrn, position, commentText, originScreen));
    }
}
