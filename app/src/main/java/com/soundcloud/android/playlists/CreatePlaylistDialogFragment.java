package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.eventbus.EventBus;
import eu.inmite.android.lib.dialogs.BaseDialogFragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import javax.inject.Inject;

public class CreatePlaylistDialogFragment extends BaseDialogFragment {

    private static final String KEY_ORIGIN_SCREEN = "ORIGIN_SCREEN";
    private static final String KEY_TRACK_ID = "TRACK_ID";

    @Inject LegacyPlaylistOperations playlistOperations;
    @Inject EventBus eventBus;
    @Inject ApplicationProperties properties;
    @Inject AccountOperations accountOperations;

    public static CreatePlaylistDialogFragment from(long trackId, String originScreen) {
        Bundle b = new Bundle();
        b.putLong(KEY_TRACK_ID, trackId);
        b.putString(KEY_ORIGIN_SCREEN, originScreen);
        CreatePlaylistDialogFragment fragment = new CreatePlaylistDialogFragment();
        fragment.setArguments(b);
        return fragment;
    }

    public CreatePlaylistDialogFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected Builder build(Builder initialBuilder) {
        final View dialogView = View.inflate(getActivity(), R.layout.dialog_create_new_playlist, null);
        final EditText input = (EditText) dialogView.findViewById(android.R.id.edit);
        final CheckBox privacy = (CheckBox) dialogView.findViewById(R.id.chk_private);

        initialBuilder.setTitle(R.string.create_new_playlist);
        initialBuilder.setView(dialogView);

        if (!properties.isDevBuildRunningOnDevice()){
            privacy.setVisibility(View.GONE);
        }

        initialBuilder.setNegativeButton(R.string.cancel,new View.OnClickListener() {
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
                    createPlaylist(playlistTitle, properties.isDevBuildRunningOnDevice() && privacy.isChecked());
                    Toast.makeText(CreatePlaylistDialogFragment.this.getActivity(), R.string.added_to_playlist, Toast.LENGTH_SHORT).show();
                    getDialog().dismiss();
                }
            }
        });
        return initialBuilder;
    }

    private void createPlaylist(final String title, final boolean isPrivate) {
        final PublicApiUser currentUser = accountOperations.getLoggedInUser();
        final long firstTrackId = getArguments().getLong(KEY_TRACK_ID);
        final String originScreen = getArguments().getString(KEY_ORIGIN_SCREEN);
        fireAndForget(playlistOperations.createNewPlaylist(currentUser, title, isPrivate, firstTrackId));
        eventBus.publish(EventQueue.UI_TRACKING, UIEvent.fromAddToPlaylist(originScreen, true, firstTrackId));
    }
}
