package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.eventbus.EventBus;
import eu.inmite.android.lib.dialogs.BaseDialogFragment;
import rx.Observable;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import javax.inject.Inject;

public class CreatePlaylistDialogFragment extends BaseDialogFragment {

    private static final String CREATE_PLAYLIST_DIALOG_TAG = "create_new_set_dialog";

    private static final String KEY_INVOKER_SCREEN = "INVOKER_SCREEN";
    private static final String KEY_CONTEXT_SCREEN = "ORIGIN_SCREEN";
    private static final String KEY_TRACK_ID = "TRACK_ID";

    @Inject PlaylistOperations playlistOperations;
    @Inject EventBus eventBus;
    @Inject ApplicationProperties properties;
    @Inject FeatureOperations featureOperations;

    @InjectView(android.R.id.edit) EditText input;
    @InjectView(R.id.chk_private) CheckBox privacy;
    @InjectView(R.id.chk_offline) CheckBox offline;

    public static CreatePlaylistDialogFragment from(long trackId, String invokerScreen, String contextScreen) {
        return createFragment(createBundle(trackId, invokerScreen, contextScreen));
    }

    private static Bundle createBundle(long trackId, String invokerScreen, String contextScreen) {
        Bundle bundle = new Bundle();
        bundle.putLong(KEY_TRACK_ID, trackId);
        bundle.putString(KEY_INVOKER_SCREEN, invokerScreen);
        bundle.putString(KEY_CONTEXT_SCREEN, contextScreen);
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
    protected Builder build(Builder initialBuilder) {
        final View dialogView = View.inflate(getActivity(), R.layout.dialog_create_new_playlist, null);
        ButterKnife.inject(this, dialogView);

        initialBuilder.setTitle(R.string.create_new_playlist);
        initialBuilder.setView(dialogView);

        setChecksVisibility();
        initialBuilder.setNegativeButton(R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });
        initialBuilder.setPositiveButton(R.string.done, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String playlistTitle = input.getText().toString().trim();
                if (TextUtils.isEmpty(playlistTitle)) {
                    Toast.makeText(getActivity(), R.string.error_new_playlist_blank_title, Toast.LENGTH_SHORT).show();
                } else {
                    createPlaylist(playlistTitle, properties.isDevBuildRunningOnDevice() && privacy.isChecked(), offline.isChecked());
                    Toast.makeText(CreatePlaylistDialogFragment.this.getActivity(), R.string.added_to_playlist, Toast.LENGTH_SHORT).show();
                    getDialog().dismiss();
                }
            }
        });
        return initialBuilder;
    }

    private void setChecksVisibility() {
        if (!featureOperations.isOfflineContentEnabled()) {
            offline.setVisibility(View.GONE);
        }
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, CREATE_PLAYLIST_DIALOG_TAG);
    }

    private void createPlaylist(final String title, final boolean isPrivate, final boolean isOffline) {
        final long firstTrackId = getArguments().getLong(KEY_TRACK_ID);
        Observable<?> observable = isOffline
                ? playlistOperations.createNewOfflinePlaylist(title, isPrivate, Urn.forTrack(firstTrackId))
                : playlistOperations.createNewPlaylist(title, isPrivate, Urn.forTrack(firstTrackId));

        fireAndForget(observable);
        trackAddingToPlaylistEvent(firstTrackId);
    }

    private void trackAddingToPlaylistEvent(long trackId) {
        final String invokerScreen = getArguments().getString(KEY_INVOKER_SCREEN);
        final String contextScreen = getArguments().getString(KEY_CONTEXT_SCREEN);
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromAddToPlaylist(invokerScreen, contextScreen, true, trackId));
    }

}
