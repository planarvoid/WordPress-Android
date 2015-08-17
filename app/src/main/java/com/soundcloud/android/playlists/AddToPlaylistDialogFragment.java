package com.soundcloud.android.playlists;

import static rx.android.app.AppObservable.bindFragment;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Subscription;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;
import java.util.List;

public class AddToPlaylistDialogFragment extends DialogFragment {

    private static final String PLAYLIST_DIALOG_TAG = "create_playlist_dialog";

    private static final String KEY_CONTEXT_SCREEN = "CONTEXT_SCREEN";
    private static final String KEY_TRACK_ID = "TRACK_ID";
    private static final String KEY_TRACK_TITLE = "TRACK_TITLE";
    private static final String KEY_INVOKER_SCREEN = "INVOKER_LOCATION";

    @Inject PlaylistOperations playlistOperations;
    @Inject FeatureOperations featureOperations;
    @Inject EventBus eventBus;

    private MyPlaylistsAdapter adapter;
    private Subscription addTrackSubscription = RxUtils.invalidSubscription();
    private Subscription loadPlaylistSubscription = RxUtils.invalidSubscription();
    private Observable<List<AddTrackToPlaylistItem>> loadPlaylists;

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
        adapter = new MyPlaylistsAdapter(getActivity(), featureOperations);
        loadPlaylistSubscription = loadPlaylists.subscribe(new PlaylistsLoadedSubscriber());

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_track_to_playlist)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        final long rowId = adapter.getItemId(position);
                        if (rowId == Urn.NOT_SET.getNumericId()) {
                            showPlaylistCreationScreen();
                            getDialog().dismiss();
                        } else if (getActivity() != null) {
                            onAddTrackToSet(rowId);
                        }
                    }
                })
                .setPositiveButton(R.string.cancel, null)
                .create();
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

    private void onAddTrackToSet(long playlistId) {
        long trackId = getArguments().getLong(KEY_TRACK_ID);

        addTrackSubscription = bindFragment(this,
                playlistOperations
                        .addTrackToPlaylist(Urn.forPlaylist(playlistId), Urn.forTrack(trackId)))
                        .subscribe(new TrackAddedSubscriber());

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
    }

    static class MyPlaylistsAdapter extends ListItemAdapter<AddTrackToPlaylistItem> {

        @InjectView(R.id.title) TextView titleView;
        @InjectView(R.id.trackCount) TextView trackCountView;
        @InjectView(R.id.icon_private) ImageView privateIcon;
        @InjectView(R.id.icon_offline) ImageView offlineIcon;

        private final Context context;
        private final FeatureOperations featureOperations;

        public MyPlaylistsAdapter(Context c, FeatureOperations featureOperations) {
            this.context = c;
            this.featureOperations = featureOperations;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public AddTrackToPlaylistItem getItem(int position) {
            return items.get(position);
        }

        public long getItemId(int position) {
            return getItem(position).playlistUrn.getNumericId();
        }

        @Override
        public boolean isEnabled(int position) {
            return !getItem(position).isTrackAdded;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(context, R.layout.add_to_playlist_list_item, null);
            }
            ButterKnife.inject(this, convertView);

            final AddTrackToPlaylistItem item = getItem(position);

            titleView.setEnabled(isEnabled(position));
            setTitleText(item);
            setTrackCountView(item.trackCount);
            setIconsVisibility(item.isPrivate, item.isOffline);

            return convertView;
        }

        private void setTitleText(AddTrackToPlaylistItem item) {
            if (item.playlistUrn == Urn.NOT_SET) {
                titleView.setText(context.getString(R.string.create_new_playlist));
            } else {
                titleView.setText(item.title);
            }
        }

        private void setIconsVisibility(boolean isPrivatePlaylist, boolean isOfflinePlaylist) {
            privateIcon.setVisibility(isPrivatePlaylist ? View.VISIBLE : View.GONE);

            final boolean showOfflineIcon = featureOperations.isOfflineContentEnabled() && isOfflinePlaylist;
            offlineIcon.setVisibility(showOfflineIcon ? View.VISIBLE : View.GONE);
        }

        private void setTrackCountView(int trackCount) {
            if (trackCount == Consts.NOT_SET) {
                trackCountView.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, context.getResources().getDrawable(R.drawable.ic_plus), null);
                trackCountView.setText(null);
            } else {
                trackCountView.setCompoundDrawablesWithIntrinsicBounds(
                        context.getResources().getDrawable(R.drawable.stats_sounds), null, null, null);
                trackCountView.setText(String.valueOf(trackCount));
            }
        }
    }

    private class PlaylistsLoadedSubscriber extends DefaultSubscriber<List<AddTrackToPlaylistItem>> {
        @Override
        public void onNext(List<AddTrackToPlaylistItem> playlistItems) {
            adapter.addItem(AddTrackToPlaylistItem.createNewPlaylistItem());
            for (AddTrackToPlaylistItem playlist : playlistItems) {
                adapter.addItem(playlist);
            }
            adapter.notifyDataSetChanged();
        }
    }
}
