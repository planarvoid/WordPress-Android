package com.soundcloud.android.playlists;

import static rx.android.observables.AndroidObservable.bindFragment;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.NotFoundException;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import eu.inmite.android.lib.dialogs.BaseDialogFragment;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class AddToPlaylistDialogFragment extends BaseDialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String PLAYLIST_DIALOG_TAG = "create_playlist_dialog";
    private static final String KEY_ORIGIN_SCREEN = "ORIGIN_SCREEN";
    private static final String KEY_TRACK_ID = "TRACK_ID";
    private static final String KEY_TRACK_TITLE = "TRACK_TITLE";
    private static final String COL_ALREADY_ADDED = "ALREADY_ADDED";

    private static final int LOADER_ID = 1;
    private static final int NEW_PLAYLIST_ITEM = -1;
    private static final int CLOSE_DELAY_MILLIS = 500;

    private MyPlaylistsAdapter adapter;
    private Subscription addTrackSubscription = Subscriptions.empty();

    @Inject LegacyPlaylistOperations playlistOperations;
    @Inject EventBus eventBus;
    @Inject LocalBroadcastManager broadcastManager;

    private final Action1<PublicApiPlaylist> broadcastTrackAdded = new Action1<PublicApiPlaylist>() {
        @Override
        public void call(PublicApiPlaylist playlist) {
            // TODO: move to an Rx event
            // broadcast the information that the number of tracks changed
            Intent intent = new Intent(PublicApiPlaylist.ACTION_CONTENT_CHANGED);
            intent.putExtra(PublicApiPlaylist.EXTRA_ID, playlist.getId());
            intent.putExtra(PublicApiPlaylist.EXTRA_TRACKS_COUNT, playlist.getTrackCount());

            broadcastManager.sendBroadcast(intent);
        }
    };

    public static AddToPlaylistDialogFragment from(PropertySet track, String originScreen) {
        Bundle b = new Bundle();
        b.putLong(KEY_TRACK_ID, track.get(TrackProperty.URN).getNumericId());
        b.putString(KEY_TRACK_TITLE, track.get(PlayableProperty.TITLE));
        b.putString(KEY_ORIGIN_SCREEN, originScreen);

        AddToPlaylistDialogFragment fragment = new AddToPlaylistDialogFragment();
        fragment.setArguments(b);
        return fragment;
    }

    public AddToPlaylistDialogFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected Builder build(Builder builder) {
        builder.setTitle(getString(R.string.add_track_to_playlist));
        adapter = new MyPlaylistsAdapter(getActivity());
        builder.setItems(adapter, 0, new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final long rowId = adapter.getItemId(position);
                if (rowId == NEW_PLAYLIST_ITEM) {
                    final long firstTrackId = getArguments().getLong(KEY_TRACK_ID);
                    final String originScreen = getArguments().getString(KEY_ORIGIN_SCREEN);
                    CreatePlaylistDialogFragment.from(firstTrackId, originScreen).show(getFragmentManager(), "create_new_set_dialog");
                    getDialog().dismiss();
                } else if (getActivity() != null) {
                    onAddTrackToSet(rowId, view);
                }
            }
        });
        builder.setPositiveButton(android.R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, getArguments(), this);

        return builder;
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, PLAYLIST_DIALOG_TAG);
    }

    private void onAddTrackToSet(long playlistId, View trackRowView) {
        long trackId = getArguments().getLong(KEY_TRACK_ID);

        final TextView txtTrackCount = (TextView) trackRowView.findViewById(R.id.trackCount);
        long newTracksCount = ScTextUtils.safeParseLong(String.valueOf(txtTrackCount.getText())) + 1;
        txtTrackCount.setText(String.valueOf(newTracksCount));

        addTrackSubscription = bindFragment(this,
                playlistOperations.addTrackToPlaylist(playlistId, trackId)
                        .doOnNext(broadcastTrackAdded)
                        .delay(CLOSE_DELAY_MILLIS, TimeUnit.MILLISECONDS, Schedulers.immediate())
        ).subscribe(new TrackAddedSubscriber());

        final String originScreen = getArguments().getString(KEY_ORIGIN_SCREEN);
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromAddToPlaylist(originScreen, false, trackId));
    }

    @Override
    public void onDestroy() {
        addTrackSubscription.unsubscribe();
        super.onDestroy();
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
                final String existsCol = "EXISTS (SELECT 1 FROM " + Table.PlaylistTracks
                        + " WHERE " + TableColumns.PlaylistTracks.TRACK_ID + " = " + getArguments().getLong(KEY_TRACK_ID) + " AND " +
                        TableColumns.PlaylistTracks.PLAYLIST_ID + " = " + TableColumns.PlaylistTracksView._ID + ") as " + COL_ALREADY_ADDED;

                Cursor dbCursor = getContext().getContentResolver().query(
                        Content.ME_PLAYLISTS.uri,
                        new String[]{TableColumns.PlaylistTracksView._ID,
                                TableColumns.PlaylistTracksView.TITLE,
                                TableColumns.PlaylistTracksView.TRACK_COUNT,
                                existsCol},
                        null, null, null);

                MatrixCursor extras = new MatrixCursor(new String[]{TableColumns.PlaylistTracksView._ID,
                        TableColumns.PlaylistTracksView.TITLE, TableColumns.PlaylistTracksView.TRACK_COUNT, COL_ALREADY_ADDED});

                extras.addRow(new Object[]{NEW_PLAYLIST_ITEM, getContext().getString(R.string.create_new_playlist), -1, 0});

                return new MergeCursor(new Cursor[]{extras, dbCursor});
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.setCursor(null);
    }

    private final class TrackAddedSubscriber extends DefaultSubscriber<PublicApiPlaylist> {

        @Override
        public void onNext(PublicApiPlaylist playlist) {
            final Dialog toDismiss = getDialog();
            Toast.makeText(getActivity(), R.string.added_to_playlist, Toast.LENGTH_SHORT).show();
            if (toDismiss != null) {
                toDismiss.dismiss();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof NotFoundException) {
                Toast.makeText(getActivity(), getString(R.string.playlist_removed), Toast.LENGTH_SHORT).show();
            } else {
                super.onError(e);
            }
        }
    }

    private static class MyPlaylistsAdapter extends BaseAdapter {
        private final Context context;
        private Cursor cursor;

        public MyPlaylistsAdapter(Context c) {
            context = c;
        }

        @Override
        public int getCount() {
            return cursor == null ? 0 : cursor.getCount();
        }

        public Object getItem(int position) {
            if (cursor == null) {
                return null;
            } else {
                cursor.moveToPosition(position);
                return cursor;
            }
        }

        public long getItemId(int position) {
            if (cursor != null && cursor.moveToPosition(position)) {
                return cursor.getLong(cursor.getColumnIndex(TableColumns.PlaylistTracksView._ID));
            } else {
                return 0;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            if (cursor != null && cursor.moveToPosition(position)) {
                return cursor.getInt(cursor.getColumnIndex(COL_ALREADY_ADDED)) != 1;
            } else {
                return false;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(context, R.layout.add_to_playlist_list_item, null);
            }

            if (cursor.moveToPosition(position)) {
                final TextView txtTitle = (TextView) convertView.findViewById(R.id.title);
                final TextView txtTrackCount = ((TextView) convertView.findViewById(R.id.trackCount));

                // text colors
                final boolean alreadyAdded = (cursor.getInt(cursor.getColumnIndex(COL_ALREADY_ADDED)) == 1);
                txtTitle.setEnabled(!alreadyAdded);

                txtTitle.setText(cursor.getString(cursor.getColumnIndex(TableColumns.PlaylistTracksView.TITLE)));
                final int trackCount = cursor.getInt(cursor.getColumnIndex(TableColumns.PlaylistTracksView.TRACK_COUNT));
                if (trackCount == -1) {
                    txtTrackCount.setCompoundDrawablesWithIntrinsicBounds(
                            null, null, context.getResources().getDrawable(R.drawable.ic_plus), null);
                    txtTrackCount.setText(null);
                } else {
                    txtTrackCount.setCompoundDrawablesWithIntrinsicBounds(
                            context.getResources().getDrawable(R.drawable.stats_sounds), null, null, null);
                    txtTrackCount.setText(String.valueOf(trackCount));
                }
            }
            return convertView;
        }

        public void setCursor(Cursor data) {
            cursor = data;
            notifyDataSetChanged();
        }
    }
}
