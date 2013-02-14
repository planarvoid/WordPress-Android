package com.soundcloud.android.dialog;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.provider.Table;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MyPlaylistsDialogFragment extends SherlockDialogFragment {

    public static final String KEY_TRACK_ID = "TRACK_ID";
    public static final String KEY_TRACK_TITLE = "TRACK_TITLE";

    public static final String COL_ALREADY_ADDED = "ALREADY_ADDED";

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

        final BaseAdapter adapter = new MyPlaylistsAdapter(getActivity(),getArguments().getLong(KEY_TRACK_ID));
        final View dialogView = View.inflate(getActivity(), R.layout.alert_dialog_title_listview, null);
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
                            Log.e(SoundCloudApplication.TAG, "Could not parse track count of " + txtTrackCount.getText(), e);
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

        ((TextView) dialogView.findViewById(android.R.id.title)).setText(getString(R.string.add_track_to_set));
        ((ListView) dialogView.findViewById(android.R.id.list)).setAdapter(adapter);

        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setView(dialogView);

        return builder.create();

    }

    private static class MyPlaylistsAdapter extends BaseAdapter {
        private Context mContext;
        private Cursor mCursor;

        public MyPlaylistsAdapter(Context c, long trackId){
            mContext = c;
            mCursor = getCursor(trackId);
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        public Object getItem(int position) {
            mCursor.moveToPosition(position);
            return mCursor;
        }

        public long getItemId(int position) {
            if (mCursor.moveToPosition(position)) {
                return mCursor.getLong(mCursor.getColumnIndex(DBHelper.PlaylistTracksView._ID));
            } else {
                return 0;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            if (mCursor.moveToPosition(position)) {
                return mCursor.getInt(mCursor.getColumnIndex(COL_ALREADY_ADDED)) != 1;
            } else {
                return false;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = View.inflate(mContext, R.layout.pick_set_row, null);
            }

            if (mCursor.moveToPosition(position)){
                final TextView txtTitle = (TextView) convertView.findViewById(R.id.title);
                final TextView txtTrackCount = ((TextView) convertView.findViewById(R.id.trackCount));

                // text colors
                final boolean alreadyAdded = (mCursor.getInt(mCursor.getColumnIndex(COL_ALREADY_ADDED)) == 1);
                final int textColor = mContext.getResources().getColor((alreadyAdded ?
                        R.color.dialog_list_txt_disabled : R.color.light_gray_text));
                txtTitle.setTextColor(textColor);
                txtTrackCount.setTextColor(textColor);

                txtTitle.setText(mCursor.getString(mCursor.getColumnIndex(DBHelper.PlaylistTracksView.TITLE)));
                final int trackCount = mCursor.getInt(mCursor.getColumnIndex(DBHelper.PlaylistTracksView.TRACK_COUNT));
                if (trackCount == -1) {
                    txtTrackCount.setCompoundDrawablesWithIntrinsicBounds(
                            mContext.getResources().getDrawable(R.drawable.ic_new_set), null, null, null);
                    txtTrackCount.setText("");
                } else {
                    txtTrackCount.setCompoundDrawablesWithIntrinsicBounds(
                            mContext.getResources().getDrawable(R.drawable.stream_white_sm), null, null, null);
                    txtTrackCount.setText(String.valueOf(trackCount));
                }
            }
            return convertView;
        }

        private Cursor getCursor(long trackId) {
            final String existsCol = "EXISTS (SELECT 1 FROM " + Table.PLAYLIST_TRACKS
                    + " WHERE " + DBHelper.PlaylistTracks.TRACK_ID + " = " + trackId + " AND " +
                    DBHelper.PlaylistTracks.PLAYLIST_ID + " = " + DBHelper.PlaylistTracksView._ID + ") as " + COL_ALREADY_ADDED;

            Cursor dbCursor = mContext.getContentResolver().query(
                    Content.ME_PLAYLISTS.uri,
                    new String[]{DBHelper.PlaylistTracksView._ID,
                            DBHelper.PlaylistTracksView.TITLE,
                            DBHelper.PlaylistTracksView.TRACK_COUNT,
                            existsCol},
                    null, null, null);

            MatrixCursor extras = new MatrixCursor(new String[]{DBHelper.PlaylistTracksView._ID,
                    DBHelper.PlaylistTracksView.TITLE, DBHelper.PlaylistTracksView.TRACK_COUNT, COL_ALREADY_ADDED});

            extras.addRow(new Object[]{-1l, mContext.getString(R.string.create_new_set), -1, 0});

            return new MergeCursor(new Cursor[]{extras, dbCursor});
        }
    }
}
