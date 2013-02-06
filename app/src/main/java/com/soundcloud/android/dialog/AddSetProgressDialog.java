package com.soundcloud.android.dialog;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.create.NewPlaylistTask;
import com.soundcloud.api.Request;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

public class AddSetProgressDialog extends SherlockDialogFragment {
    public static final String KEY_TRACK_ID = "TRACK_ID";
    public static final String KEY_PLAYLIST_TITLE = "TITLE";
    public static final String KEY_PLAYLIST_PRIVATE = "PRIVACY";

    public static AddSetProgressDialog from(long trackId, String title, boolean isPrivate) {

        Bundle b = new Bundle();
        b.putLong(KEY_TRACK_ID, trackId);
        b.putString(KEY_PLAYLIST_TITLE, title);
        b.putBoolean(KEY_PLAYLIST_PRIVATE, isPrivate);

        AddSetProgressDialog fragment = new AddSetProgressDialog();
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog.Builder builder = new ProgressDialog.Builder(
                new ContextThemeWrapper(getActivity(), R.style.ScDialog)
        );

        final View dialogView = View.inflate(getActivity(), R.layout.alert_dialog_title_progress, null);
        final TextView title = (TextView) dialogView.findViewById(android.R.id.title);
        final ProgressBar progressBar = (ProgressBar) dialogView.findViewById(R.id.progress);

        title.setText(getString(R.string.creating_set, getArguments().getString(KEY_PLAYLIST_TITLE)));
        progressBar.setIndeterminate(true);
        builder.setView(dialogView);

        return builder.create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Playlist.ApiCreateObject uploadObject = new Playlist.ApiCreateObject(
                getArguments().getString(KEY_PLAYLIST_TITLE),
                getArguments().getLong(KEY_TRACK_ID),
                getArguments().getBoolean(KEY_PLAYLIST_PRIVATE));


        final AndroidCloudAPI application = (AndroidCloudAPI) getActivity().getApplication();
        Request r = null;
        try {
            final String content = application.getMapper().writeValueAsString(uploadObject);
            r = Request.to(TempEndpoints.PLAYLISTS).withContent(content, "application/json");

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        if (r != null){
            new NewPlaylistTask(application){
                @Override
                protected void onPostExecute(Playlist playlist) {
                    super.onPostExecute(playlist);
                    if (playlist != null){
                        // insert sounds association for new playlist
                        new SoundAssociation(playlist,new Date(System.currentTimeMillis()), SoundAssociation.Type.PLAYLIST)
                                .insert(getActivity().getContentResolver(), Content.ME_SOUNDS.uri);

                        // force me_sounds to stale so it will sync next time
                        LocalCollection.forceToStale(Content.ME_SOUNDS.uri,getActivity().getContentResolver());

                        onCompleted(true);
                    } else {
                        onCompleted(false);
                    }
                }
            }.execute(r);
        }
    }

    private void onCompleted(boolean success){
        if (!success && getActivity() != null){
            Toast.makeText(getActivity(), R.string.error_creating_new_set, Toast.LENGTH_LONG);
        }
        dismiss();
    }

}
