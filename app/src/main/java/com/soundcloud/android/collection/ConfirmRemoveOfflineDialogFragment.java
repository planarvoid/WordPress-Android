package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.rx.observers.DefaultCompletableObserver;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

import javax.inject.Inject;

public class ConfirmRemoveOfflineDialogFragment extends DialogFragment {

    private static final String TAG = "RemoveOffline";
    private static final String PLAYLIST_URN = "PlaylistUrn";
    private static final String PROMOTED_SOURCE_INFO = "PromotedSourceInfo";

    @Inject OfflineContentOperations offlineContentOperations;
    @Inject EventBus eventBus;
    @Inject ScreenProvider screenProvider;
    @Inject LeakCanaryWrapper leakCanaryWrapper;
    @Inject ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    public static void showForPlaylist(FragmentManager fragmentManager,
                                       Urn playlist,
                                       PromotedSourceInfo promotedSourceInfo) {
        ConfirmRemoveOfflineDialogFragment fragment = new ConfirmRemoveOfflineDialogFragment();
        Bundle bundle = new Bundle();
        Urns.writeToBundle(bundle, PLAYLIST_URN, playlist);
        bundle.putParcelable(PROMOTED_SOURCE_INFO, promotedSourceInfo);
        fragment.setArguments(bundle);
        fragment.show(fragmentManager, TAG);
    }

    public static void showForLikes(FragmentManager fragmentManager) {
        new ConfirmRemoveOfflineDialogFragment().show(fragmentManager, TAG);
    }

    public ConfirmRemoveOfflineDialogFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(R.string.disable_offline_collection_from_context_title)
                .setMessage(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.DISABLE_OFFLINE_COLLECTION_FROM_CONTEXT_BODY)).get();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    disableAutomaticCollectionSync();
                    proceedWithRemoval();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    private void disableAutomaticCollectionSync() {
        offlineContentOperations.disableOfflineCollection();
        Optional<Urn> playlist = isForPlaylist() ? Optional.of(playlistUrn()) : Optional.absent();
        eventBus.publish(EventQueue.TRACKING,
                         OfflineInteractionEvent.fromDisableCollectionSync(screenProvider.getLastScreenTag(),
                                                                           playlist));
    }

    private void proceedWithRemoval() {
        if (isForPlaylist()) {
            removeOfflinePlaylist(playlistUrn());
        } else {
            removeOfflineLikes();
        }
    }

    private void removeOfflineLikes() {
        offlineContentOperations.disableOfflineLikedTracks().subscribe(new DefaultCompletableObserver());
        eventBus.publish(EventQueue.TRACKING,
                         OfflineInteractionEvent.fromRemoveOfflineLikes(screenProvider.getLastScreenTag()));
    }

    private void removeOfflinePlaylist(Urn urn) {
        offlineContentOperations.makePlaylistUnavailableOffline(urn).subscribe(new DefaultCompletableObserver());
        eventBus.publish(EventQueue.TRACKING,
                         OfflineInteractionEvent.fromRemoveOfflinePlaylist(screenProvider.getLastScreenTag(),
                                                                           urn,
                                                                           promotedSourceInfo()));
    }

    private boolean isForPlaylist() {
        return getArguments() != null && getArguments().containsKey(PLAYLIST_URN);
    }

    private Urn playlistUrn() {
        return Urns.urnFromBundle(getArguments(), PLAYLIST_URN);
    }

    private PromotedSourceInfo promotedSourceInfo() {
        return (PromotedSourceInfo) getArguments().getParcelable(PROMOTED_SOURCE_INFO);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        leakCanaryWrapper.watch(this);
    }

}
