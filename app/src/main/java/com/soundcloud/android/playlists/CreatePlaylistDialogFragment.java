package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.rx.eventbus.EventBus;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import javax.inject.Inject;

public class CreatePlaylistDialogFragment extends DialogFragment {

    private static final String CREATE_PLAYLIST_DIALOG_TAG = "create_new_set_dialog";

    private static final String KEY_TRACK_ID = "TRACK_ID";

    @Inject PlaylistOperations playlistOperations;
    @Inject OfflineContentOperations offlineContentOperations;
    @Inject EventBus eventBus;
    @Inject ApplicationProperties properties;
    @Inject FeatureOperations featureOperations;
    @Inject OfflineSettingsStorage offlineSettingsStorage;
    @Inject LeakCanaryWrapper leakCanaryWrapper;

    @BindView(android.R.id.edit) EditText input;
    @BindView(R.id.chk_private) CheckBox privacy;
    @BindView(R.id.chk_offline) CheckBox offline;

    public static CreatePlaylistDialogFragment from(long trackId) {
        return createFragment(createBundle(trackId));
    }

    private static Bundle createBundle(long trackId) {
        Bundle bundle = new Bundle();
        bundle.putLong(KEY_TRACK_ID, trackId);
        return bundle;
    }

    private static CreatePlaylistDialogFragment createFragment(Bundle bundle) {
        CreatePlaylistDialogFragment fragment = new CreatePlaylistDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public CreatePlaylistDialogFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialogView = View.inflate(getActivity(), R.layout.dialog_create_new_playlist, null);
        ButterKnife.bind(this, dialogView);
        setOfflineVisibility();

        return new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setPositiveButton(R.string.btn_done, (dialog, which) -> {
                    final String playlistTitle = input.getText().toString().trim();
                    if (TextUtils.isEmpty(playlistTitle)) {
                        Toast.makeText(getActivity(), R.string.error_new_playlist_blank_title, Toast.LENGTH_SHORT)
                             .show();
                    } else {
                        createPlaylist(playlistTitle, privacy.isChecked(), offline.isChecked());
                        Toast.makeText(CreatePlaylistDialogFragment.this.getActivity(),
                                       R.string.added_to_playlist,
                                       Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
    }

    private void setOfflineVisibility() {
        if (featureOperations.isOfflineContentEnabled()
                && offlineSettingsStorage.isOfflineContentAccessible()
                && !offlineContentOperations.isOfflineCollectionEnabled()) {
            offline.setVisibility(View.VISIBLE);
        }
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, CREATE_PLAYLIST_DIALOG_TAG);
    }

    private void createPlaylist(final String title, final boolean isPrivate, final boolean isOffline) {
        final long firstTrackId = getArguments().getLong(KEY_TRACK_ID);
        fireAndForget(playlistOperations.createNewPlaylist(title,
                                                           isPrivate,
                                                           isOffline,
                                                           Urn.forTrack(firstTrackId)));

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromAddToPlaylist(getEventContextMetadata()));
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        leakCanaryWrapper.watch(this);
    }

    private EventContextMetadata getEventContextMetadata() {
        return EventContextMetadata.builder().build();
    }
}
