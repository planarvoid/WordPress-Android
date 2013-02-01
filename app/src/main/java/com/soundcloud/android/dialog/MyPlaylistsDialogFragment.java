package com.soundcloud.android.dialog;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.view.ButtonBar;

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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MyPlaylistsDialogFragment extends SherlockDialogFragment {

    public static final String KEY_TRACK_ID = "TRACK_ID";
    public static final String KEY_TRACK_TITLE = "TRACK_TITLE";

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

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.ScDialog));

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
                               R.drawable.ic_new_set :
                               R.drawable.ic_set);

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

        final View dialogView = View.inflate(getActivity(), R.layout.alert_dialog_title_listview, null);
        ((TextView) dialogView.findViewById(android.R.id.title)).setText(getString(R.string.add_track_to_set, getArguments().getString(KEY_TRACK_TITLE)));
        ((ListView) dialogView.findViewById(android.R.id.list)).setAdapter(adapter);

        ((ListView) dialogView.findViewById(android.R.id.list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final long playlistId = adapter.getItemId(position);

                if (playlistId == -1) {
                    CreateNewSetDialogFragment.from(getArguments().getLong(KEY_TRACK_ID)).show(getFragmentManager(), "create_new_set_dialog");
                } else {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.PlaylistTracks.PLAYLIST_ID, playlistId);
                    cv.put(DBHelper.PlaylistTracks.TRACK_ID, getArguments().getLong(KEY_TRACK_ID));
                    cv.put(DBHelper.PlaylistTracks.ADDED_AT, System.currentTimeMillis());
                    // off the UI thread??? ugh
                    getActivity().getContentResolver().insert(Content.PLAYLIST_TRACKS.forId(playlistId), cv);
                }
                // we done
                getDialog().dismiss();
            }
        });

        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setView(dialogView);

        return builder.create();

    }
}
