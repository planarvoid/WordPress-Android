package com.soundcloud.android.playlists;

import static rx.android.observables.AndroidObservable.bindFragment;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.afollestad.materialdialogs.MaterialDialog;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.NotFoundException;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AddToPlaylistDialogFragment extends DialogFragment {

    private static final String PLAYLIST_DIALOG_TAG = "create_playlist_dialog";

    private static final String KEY_CONTEXT_SCREEN = "CONTEXT_SCREEN";
    private static final String KEY_TRACK_ID = "TRACK_ID";
    private static final String KEY_TRACK_TITLE = "TRACK_TITLE";
    private static final String KEY_INVOKER_SCREEN = "INVOKER_LOCATION";

    private static final int CLOSE_DELAY_MILLIS = 500;

    private MyPlaylistsAdapter adapter;
    private Subscription addTrackSubscription = Subscriptions.empty();
    private Subscription loadPlaylistSubscription = Subscriptions.empty();

    private Observable<List<PropertySet>> loadPlaylists;
    @Inject PlaylistOperations playlistOperations;
    @Inject EventBus eventBus;

    public static AddToPlaylistDialogFragment from(Urn trackUrn, String trackTitle, String invokerScreen, String contextScreen) {
        return createFragment(createBundle(trackUrn, trackTitle, invokerScreen, contextScreen));
    }

    private static Bundle createBundle(Urn trackUrn, String trackTitle, String invokerScreen, String contextScreen) {
        Bundle bundle = new Bundle();
        bundle.putLong(KEY_TRACK_ID, trackUrn.getNumericId());
        bundle.putString(KEY_TRACK_TITLE, trackTitle);
        bundle.putString(KEY_INVOKER_SCREEN, invokerScreen);
        bundle.putString(KEY_CONTEXT_SCREEN, contextScreen);
        return bundle;
    }

    private static AddToPlaylistDialogFragment createFragment(Bundle bundle) {
        AddToPlaylistDialogFragment fragment = new AddToPlaylistDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public AddToPlaylistDialogFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Urn trackUrn = Urn.forTrack(getArguments().getLong(KEY_TRACK_ID));
        loadPlaylists = playlistOperations
                .loadPlaylistForAddingTrack(trackUrn)
                .observeOn(mainThread())
                .cache();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        adapter = new MyPlaylistsAdapter(getActivity());
        loadPlaylistSubscription = loadPlaylists.subscribe(new PlaylistLoadedSubscriber());

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.add_track_to_playlist)
                .adapter(adapter, getPlaylistCallback())
                .positiveText(R.string.cancel)
                .itemsCallback(getPlaylistCallback())
                .build();
    }

    private MaterialDialog.ListCallback getPlaylistCallback() {
        return new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog materialDialog, View view, int position, CharSequence charSequence) {
                final long rowId = adapter.getItemId(position);
                if (rowId == Urn.NOT_SET.getNumericId()) {
                    showPlaylistCreationScreen();
                    getDialog().dismiss();
                } else if (getActivity() != null) {
                    onAddTrackToSet(rowId, view);
                }
            }
        };
    }

    private void showPlaylistCreationScreen() {
        final String invokerScreen = getArguments().getString(KEY_INVOKER_SCREEN);
        final String contextScreen = getArguments().getString(KEY_CONTEXT_SCREEN);
        final long firstTrackId = getArguments().getLong(KEY_TRACK_ID);
        CreatePlaylistDialogFragment.from(firstTrackId, invokerScreen, contextScreen).show(getFragmentManager());
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
                playlistOperations.addTrackToPlaylist(Urn.forPlaylist(playlistId), Urn.forTrack(trackId))
                        .delay(CLOSE_DELAY_MILLIS, TimeUnit.MILLISECONDS, Schedulers.immediate())
        ).subscribe(new TrackAddedSubscriber());

        trackAddingToPlaylistEvent(trackId);
    }

    private void trackAddingToPlaylistEvent(long trackId) {
        final String invokerScreen = getArguments().getString(KEY_INVOKER_SCREEN);
        final String contextScreen = getArguments().getString(KEY_CONTEXT_SCREEN);
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromAddToPlaylist(invokerScreen, contextScreen, false, trackId));
    }

    @Override
    public void onDestroy() {
        addTrackSubscription.unsubscribe();
        loadPlaylistSubscription.unsubscribe();
        super.onDestroy();
    }

    private final class TrackAddedSubscriber extends DefaultSubscriber<PropertySet> {

        @Override
        public void onNext(PropertySet ignored) {
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

    private static class MyPlaylistsAdapter extends ItemAdapter<PropertySet> {
        private final Context context;

        public MyPlaylistsAdapter(Context c) {
            context = c;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        public PropertySet getItem(int position) {
            return items.get(position);
        }

        public long getItemId(int position) {
            return getItem(position).getOrElse(TrackInPlaylistProperty.URN, Urn.NOT_SET).getNumericId();
        }

        @Override
        public boolean isEnabled(int position) {
            return !getItem(position).getOrElse(TrackInPlaylistProperty.ADDED_TO_URN, false);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(context, R.layout.add_to_playlist_list_item, null);
            }

            final PropertySet item = getItem(position);
            final TextView txtTitle = (TextView) convertView.findViewById(R.id.title);
            final TextView txtTrackCount = ((TextView) convertView.findViewById(R.id.trackCount));

            // text colors
            txtTitle.setEnabled(isEnabled(position));
            txtTitle.setText(item.getOrElse(TrackInPlaylistProperty.TITLE, context.getString(R.string.create_new_playlist)));

            final int trackCount = item.getOrElse(TrackInPlaylistProperty.TRACK_COUNT, Consts.NOT_SET);
            if (trackCount == Consts.NOT_SET) {
                txtTrackCount.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, context.getResources().getDrawable(R.drawable.ic_plus), null);
                txtTrackCount.setText(null);
            } else {
                txtTrackCount.setCompoundDrawablesWithIntrinsicBounds(
                        context.getResources().getDrawable(R.drawable.stats_sounds), null, null, null);
                txtTrackCount.setText(String.valueOf(trackCount));
            }

            return convertView;
        }
    }

    private class PlaylistLoadedSubscriber extends DefaultSubscriber<List<PropertySet>> {
        @Override
        public void onNext(List<PropertySet> args) {
            adapter.addItem(PropertySet.create());
            for (PropertySet p : args) {
                adapter.addItem(p);
            }
            adapter.notifyDataSetChanged();
        }
    }
}
