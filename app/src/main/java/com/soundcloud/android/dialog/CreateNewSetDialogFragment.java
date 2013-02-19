package com.soundcloud.android.dialog;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.provider.Content;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

public class CreateNewSetDialogFragment extends SherlockDialogFragment {

    public static final String KEY_TRACK_ID = "TRACK_ID";

    public static CreateNewSetDialogFragment from(long trackId) {

        Bundle b = new Bundle();
        b.putLong(KEY_TRACK_ID, trackId);

        CreateNewSetDialogFragment fragment = new CreateNewSetDialogFragment();
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo_Dialog));

        final View dialogView = View.inflate(getActivity(), R.layout.alert_dialog_create_new_set, null);
        ((TextView) dialogView.findViewById(android.R.id.title)).setText(R.string.create_new_set);

        // Set an EditText view to get user input
        final EditText input = (EditText) dialogView.findViewById(android.R.id.edit);
        builder.setView(dialogView);

        final CheckBox privacy = (CheckBox) dialogView.findViewById(R.id.chk_private);

        final boolean isJon = SoundCloudApplication.getUserId() == 172720l;
        if (!isJon){
            privacy.setVisibility(View.GONE);
        }

        builder.setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.done, null);

        // convoluted, but seems there's no better way:
        // http://stackoverflow.com/questions/2620444/how-to-prevent-a-dialog-from-closing-when-a-button-is-clicked
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface di) {
                Button button = dialog.getButton(Dialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (TextUtils.isEmpty(input.getText())) {
                            Toast.makeText(getActivity(), R.string.error_new_set_blank_title, Toast.LENGTH_SHORT).show();
                        } else {

                            // Commit the playlist locally in the background
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Uri uri = SoundCloudApplication.MODEL_MANAGER.createPlaylist(
                                            ((SoundCloudApplication) getActivity().getApplication()).getLoggedInUser(),
                                            String.valueOf(input.getText()),
                                            (isJon ? privacy.isChecked() : false),
                                            getArguments().getLong(KEY_TRACK_ID)
                                    );

                                    Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylist(uri);

                                    // association so it appears in ME_SOUNDS, ME_PLAYLISTS, etc.
                                    new SoundAssociation(p, new Date(System.currentTimeMillis()), SoundAssociation.Type.PLAYLIST)
                                            .insert(getActivity().getContentResolver(), Content.ME_PLAYLISTS.uri);
                                }
                            }).start();

                            dialog.dismiss();
                        }
                    }
                });
            }
        });

        return dialog;
    }

}
