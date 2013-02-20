package com.soundcloud.android.dialog;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
                            final ContentResolver contentResolver = getActivity().getContentResolver();
                            final User loggedInUser = ((SoundCloudApplication) getActivity().getApplication()).getLoggedInUser();
                            final Account account = ((SoundCloudApplication) getActivity().getApplication()).getAccount();
                            // Commit the playlist locally in the background
                            new Thread(){
                                @Override
                                public void run() {
                                    // create and insert playlist
                                    SoundCloudApplication.MODEL_MANAGER.createPlaylist(
                                            loggedInUser,
                                            String.valueOf(input.getText()),
                                            (isJon && privacy.isChecked()),
                                            getArguments().getLong(KEY_TRACK_ID)
                                    ).insertAsMyPlaylist(contentResolver);

                                    // force to stale so we know to update the playlists next time it is viewed
                                    LocalCollection.forceToStale(Content.ME_PLAYLISTS.uri,contentResolver);
                                    // request sync to push playlist at next possible opportunity
                                    ContentResolver.requestSync(account, ScContentProvider.AUTHORITY, new Bundle());

                                }
                            }.start();

                            dialog.dismiss();
                        }
                    }
                });
            }
        });

        return dialog;
    }

}
