package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

public class ClearPlayHistoryDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String TAG = "ClearPlayHistory";

    interface Listener {
        void onClearConfirmationClicked();
    }

    private Listener listener;

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    public ClearPlayHistoryDialog setListener(Listener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (listener != null) {
            listener.onClearConfirmationClicked();
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(R.string.collections_play_history_clear_dialog_title)
                .setMessage(R.string.collections_play_history_clear_dialog_message).get();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.collections_play_history_clear_dialog_button, this)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
    }
}
