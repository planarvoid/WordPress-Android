package com.soundcloud.android.dialog;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.view.ButtonBar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
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
                new String[]{DBHelper.PlaylistTracksView._ID,
                        DBHelper.PlaylistTracksView.TITLE,
                        DBHelper.PlaylistTracksView.TRACK_COUNT}, null, null, null);

        MatrixCursor extras = new MatrixCursor(new String[] { DBHelper.PlaylistTracksView._ID,
                DBHelper.PlaylistTracksView.TITLE, DBHelper.PlaylistTracksView.TRACK_COUNT });

        extras.addRow(new Object[] { -1l, getString(R.string.create_new_set), -1 });

        SimpleCursorAdapter.ViewBinder viewBinder = new SimpleCursorAdapter.ViewBinder() {
               public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                   if (columnIndex == cursor.getColumnIndex(DBHelper.PlaylistTracksView.TRACK_COUNT)){
                       final int trackCount = cursor.getInt(columnIndex);
                       final TextView txtTrackCount = (TextView) view;
                       if (trackCount == -1) {
                           txtTrackCount.setCompoundDrawablesWithIntrinsicBounds(
                                   getResources().getDrawable(R.drawable.ic_new_set), null, null, null);
                           txtTrackCount.setText("");
                       } else {
                           txtTrackCount.setCompoundDrawablesWithIntrinsicBounds(
                                   getResources().getDrawable(R.drawable.stream_white_sm), null, null, null);
                           txtTrackCount.setText(String.valueOf(cursor.getLong(columnIndex)));
                       }


                       return true;

                   } else {
                       return false;
                   }
               }
           };

        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                R.layout.pick_set_row,
                new MergeCursor(new Cursor[]{ extras, cursor }),
                new String[]{DBHelper.PlaylistTracksView.TRACK_COUNT,DBHelper.PlaylistTracksView.TITLE},
                new int[]{R.id.trackCount,R.id.title});

        adapter.setViewBinder(viewBinder);

        final View dialogView = View.inflate(getActivity(), R.layout.alert_dialog_title_listview, null);
        ((TextView) dialogView.findViewById(android.R.id.title)).setText(getString(R.string.add_track_to_set, getArguments().getString(KEY_TRACK_TITLE)));
        ((ListView) dialogView.findViewById(android.R.id.list)).setAdapter(adapter);

        final Handler handler = new Handler();

        ((ListView) dialogView.findViewById(android.R.id.list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final long playlistId = adapter.getItemId(position);

                if (playlistId == -1) {
                    CreateNewSetDialogFragment.from(getArguments().getLong(KEY_TRACK_ID)).show(getFragmentManager(), "create_new_set_dialog");
                    getDialog().dismiss();
                } else {
                    final FragmentActivity activity = getActivity();
                    if (getActivity() != null){



                        Playlist.addTrackToPlaylist(
                                activity.getContentResolver(),playlistId,getArguments().getLong(KEY_TRACK_ID));

                        // tell the service to update the playlist
                        final SoundCloudApplication soundCloudApplication = SoundCloudApplication.fromContext(getActivity());
                        if (soundCloudApplication != null){
                            ContentResolver.requestSync(soundCloudApplication.getAccount(), ScContentProvider.AUTHORITY, new Bundle());
                        }

                        final TextView txtTrackCount = (TextView) view.findViewById(R.id.trackCount);
                        try {
                            txtTrackCount.setText(String.valueOf(Integer.parseInt(String.valueOf(txtTrackCount.getText())) + 1));
                        } catch (NumberFormatException e){
                            Log.e(SoundCloudApplication.TAG,"Could not parse track count of "+ txtTrackCount.getText(), e );
                        }

                        handler.postDelayed(new Runnable() {
                            public void run() {
                                getDialog().dismiss();
                            }
                        }, 500);

                    }
                }
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
