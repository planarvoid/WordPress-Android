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
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MyPlaylistsDialogFragment extends SherlockDialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String KEY_TRACK_ID = "TRACK_ID";
    public static final String KEY_TRACK_TITLE = "TRACK_TITLE";

    public static final String COL_ALREADY_ADDED = "ALREADY_ADDED";

    private static final int LOADER_ID = 1;

    private MyPlaylistsAdapter mAdapter;

    public static MyPlaylistsDialogFragment from(Track track) {

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
        mAdapter = new MyPlaylistsAdapter(getActivity());


        final View dialogView = View.inflate(getActivity(), R.layout.alert_dialog_add_to_set, null);
        final ListView listView = (ListView) dialogView.findViewById(android.R.id.list);
        listView.setEmptyView(dialogView.findViewById(android.R.id.empty));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final long playlistId = mAdapter.getItemId(position);

                if (playlistId == -1) {
                    CreateNewSetDialogFragment.from(getArguments().getLong(KEY_TRACK_ID)).show(getFragmentManager(), "create_new_set_dialog");
                    getDialog().dismiss();
                } else if (getActivity() != null) {
                    onAddTrackToSet(playlistId, view);
                }
            }
        });

        ((TextView) dialogView.findViewById(android.R.id.title)).setText(getString(R.string.add_track_to_set));
        listView.setAdapter(mAdapter);

        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setView(dialogView);

        getSherlockActivity().getSupportLoaderManager().restartLoader(LOADER_ID, getArguments(), this);

        return builder.create();

    }

    private void onAddTrackToSet(long playlistId, View view) {
        Playlist.addTrackToPlaylist(
                getActivity().getContentResolver(), playlistId, getArguments().getLong(KEY_TRACK_ID));

        final Playlist playlist = SoundCloudApplication.MODEL_MANAGER.getPlaylist(playlistId);
        playlist.track_count++;

        // tell the service to update the playlist
        final SoundCloudApplication soundCloudApplication = SoundCloudApplication.fromContext(getActivity());
        if (soundCloudApplication != null) {
            ContentResolver.requestSync(soundCloudApplication.getAccount(), ScContentProvider.AUTHORITY, new Bundle());
        }

        final TextView txtTrackCount = (TextView) view.findViewById(R.id.trackCount);
        int newTracksCount = Integer.parseInt(String.valueOf(txtTrackCount.getText())) + 1;
        try {
            txtTrackCount.setText(String.valueOf(newTracksCount));
        } catch (NumberFormatException e) {
            Log.e(SoundCloudApplication.TAG, "Could not parse track count of " + txtTrackCount.getText(), e);
        }

        // broadcast the information that the number of tracks changed
        Intent intent = new Intent(Playlist.ACTION_CONTENT_CHANGED);
        intent.putExtra(Playlist.EXTRA_ID, playlistId);
        intent.putExtra(Playlist.EXTRA_TRACKS_COUNT, newTracksCount);
        getActivity().sendBroadcast(intent);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                getDialog().dismiss();
            }
        }, 500);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<Cursor>(getActivity()) {
            @Override
            protected void onStartLoading() {
                forceLoad();
            }
            @Override
            public Cursor loadInBackground() {
                final String existsCol = "EXISTS (SELECT 1 FROM " + Table.PLAYLIST_TRACKS
                        + " WHERE " + DBHelper.PlaylistTracks.TRACK_ID + " = " + getArguments().getLong(KEY_TRACK_ID) + " AND " +
                        DBHelper.PlaylistTracks.PLAYLIST_ID + " = " + DBHelper.PlaylistTracksView._ID + ") as " + COL_ALREADY_ADDED;

                Cursor dbCursor = getContext().getContentResolver().query(
                        Content.ME_PLAYLISTS.uri,
                        new String[]{DBHelper.PlaylistTracksView._ID,
                                DBHelper.PlaylistTracksView.TITLE,
                                DBHelper.PlaylistTracksView.TRACK_COUNT,
                                existsCol},
                        null, null, null);

                MatrixCursor extras = new MatrixCursor(new String[]{DBHelper.PlaylistTracksView._ID,
                        DBHelper.PlaylistTracksView.TITLE, DBHelper.PlaylistTracksView.TRACK_COUNT, COL_ALREADY_ADDED});

                extras.addRow(new Object[]{-1l, getContext().getString(R.string.create_new_set), -1, 0});

                return new MergeCursor(new Cursor[]{extras, dbCursor});
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.setCursor(null);
    }

    private static class MyPlaylistsAdapter extends BaseAdapter {
        private Context mContext;
        private Cursor mCursor;

        public MyPlaylistsAdapter(Context c) {
            mContext = c;
        }

        @Override
        public int getCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }

        public Object getItem(int position) {
            if (mCursor == null){
                return null;
            } else {
                mCursor.moveToPosition(position);
                return mCursor;
            }
        }

        public long getItemId(int position) {
            if (mCursor != null && mCursor.moveToPosition(position)) {
                return mCursor.getLong(mCursor.getColumnIndex(DBHelper.PlaylistTracksView._ID));
            } else {
                return 0;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            if (mCursor != null && mCursor.moveToPosition(position)) {
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

            if (mCursor.moveToPosition(position)) {
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

        public void setCursor(Cursor data) {
            mCursor = data;
            notifyDataSetChanged();
        }
    }



}
