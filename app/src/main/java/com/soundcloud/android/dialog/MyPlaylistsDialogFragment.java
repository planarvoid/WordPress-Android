package com.soundcloud.android.dialog;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;

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

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo_Dialog));

        builder.setTitle(
                getString(R.string.add_track_to_set,getArguments().getString(KEY_TRACK_TITLE))
        );

        Cursor cursor = getActivity().getContentResolver().query(
                Content.ME_PLAYLISTS.uri,
                new String[]{DBHelper.PlaylistTracksView._ID, DBHelper.PlaylistTracksView.TITLE}, null, null, null);

        MatrixCursor extras = new MatrixCursor(new String[] { DBHelper.PlaylistTracksView._ID, DBHelper.PlaylistTracksView.TITLE });
        extras.addRow(new Object[] { -1l, getString(R.string.create_new_set) });

        SimpleCursorAdapter.ViewBinder viewBinder = new SimpleCursorAdapter.ViewBinder() {
               public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                   if (columnIndex == cursor.getColumnIndex(DBHelper.PlaylistTracksView._ID)){
                       ImageView image = (ImageView) view;
                       image.setImageResource(cursor.getLong(columnIndex) == -1 ?
                               R.drawable.ic_add_to_white :
                               R.drawable.ic_user_tab_sounds);

                       return true;

                   } else {
                       return false;
                   }
               }
           };

        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                R.layout.search_suggestion,
                new MergeCursor(new Cursor[]{ extras, cursor }),
                new String[]{DBHelper.PlaylistTracksView._ID,DBHelper.PlaylistTracksView.TITLE},
                new int[]{R.id.icon,R.id.title});

        adapter.setViewBinder(viewBinder);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final long playlistId = adapter.getItemId(which);

                if (playlistId == -1){
                    CreateNewSetDialogFragment.from(getArguments().getLong(KEY_TRACK_ID)).show(getFragmentManager(),"create_new_set_dialog");
                } else {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.PlaylistTracks.PLAYLIST_ID, playlistId);
                    cv.put(DBHelper.PlaylistTracks.TRACK_ID, getArguments().getLong(KEY_TRACK_ID));
                    cv.put(DBHelper.PlaylistTracks.ADDED_AT, System.currentTimeMillis());
                    // off the UI thread??? ugh
                    getActivity().getContentResolver().insert(Content.PLAYLIST_TRACKS.forId(playlistId), cv);
                }
                // we done
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return builder.create();

    }
}
