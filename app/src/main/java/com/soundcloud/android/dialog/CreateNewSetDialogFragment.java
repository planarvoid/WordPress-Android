package com.soundcloud.android.dialog;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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

        final View dialogView = View.inflate(getActivity(), R.layout.alert_dialog_title_textview, null);
        ((TextView) dialogView.findViewById(android.R.id.title)).setText(R.string.create_new_set);

        // Set an EditText view to get user input
        final EditText input = (EditText) dialogView.findViewById(android.R.id.edit);
        builder.setView(dialogView);

        builder.setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.done,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //todo, add set logic
                dialog.dismiss();
            }
        });

        return builder.create();


    }

}
