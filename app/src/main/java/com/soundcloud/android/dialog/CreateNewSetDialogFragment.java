package com.soundcloud.android.dialog;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

import android.app.AlertDialog;
import android.app.Dialog;
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
                            AddSetProgressDialog.from(getArguments().getLong(KEY_TRACK_ID), String.valueOf(input.getText()),
                                    (isJon ? privacy.isChecked() : false))
                                    .show(getFragmentManager(), "add_set_progress");
                            dialog.dismiss();
                        }
                    }
                });
            }
        });

        return dialog;
    }

}
