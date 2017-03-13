package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.model.Urn;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

import javax.inject.Inject;

public class DeletePlaylistDialogFragment extends DialogFragment {

    private static final String TAG = "DeletePlaylist";
    private static final String PLAYLIST_URN = "PlaylistUrn";
    @Inject PlaylistPostOperations operations;

    public static void show(FragmentManager fragmentManager, Urn playlist) {
        DeletePlaylistDialogFragment fragment = new DeletePlaylistDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(PLAYLIST_URN, playlist);
        fragment.setArguments(bundle);
        fragment.show(fragmentManager, TAG);
    }

    public DeletePlaylistDialogFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(R.string.dialog_playlist_delete_title)
                .setMessage(R.string.dialog_playlist_delete_message).get();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.delete_playlist, (dialog, which) -> deletePlaylist())
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
    }

    private void deletePlaylist() {
        fireAndForget(operations.remove(urn()));
    }

    private Urn urn() {
        return (Urn) getArguments().getParcelable(PLAYLIST_URN);
    }

}
