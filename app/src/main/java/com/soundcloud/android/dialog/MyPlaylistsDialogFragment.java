package com.soundcloud.android.dialog;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;

public class MyPlaylistsDialogFragment extends SherlockDialogFragment {

    public static String KEY_TRACK_ID = "TRACK_ID";
    public static String KEY_TRACK_TITLE = "TRACK_TITLE";

    public static MyPlaylistsDialogFragment from(Track track){

        Bundle b = new Bundle();
        b.putLong(KEY_TRACK_ID, track.id);
        b.putString(KEY_TRACK_TITLE, track.title);

        MyPlaylistsDialogFragment fragment = new MyPlaylistsDialogFragment();
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(
                getString(com.soundcloud.android.R.string.add_track_to_set,getArguments().getString(KEY_TRACK_TITLE))
        );

        Cursor c = getActivity().getContentResolver().query(
                Content.ME_PLAYLISTS.uri,
                new String[]{DBHelper.PlaylistTracksView._ID, DBHelper.PlaylistTracksView.TITLE}, null, null, null);

        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(), R.layout.simple_list_item_1,
                c, new String[]{DBHelper.PlaylistTracksView.TITLE}, new int[]{R.id.text1});

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final long playlistId = adapter.getItemId(which);

                // TODO, new playlist

                ContentValues cv = new ContentValues();
                cv.put(DBHelper.PlaylistTracks.PLAYLIST_ID, playlistId);
                cv.put(DBHelper.PlaylistTracks.TRACK_ID, getArguments().getLong(KEY_TRACK_ID));
                cv.put(DBHelper.PlaylistTracks.ADDED_AT, System.currentTimeMillis());
                // off the UI thread??? ugh
                getActivity().getContentResolver().insert(Content.PLAYLIST_TRACKS.forId(playlistId), cv);

                // we done
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(getString(com.soundcloud.android.R.string.cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return builder.create();

    }
}
