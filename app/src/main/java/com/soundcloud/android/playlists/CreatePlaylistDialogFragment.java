package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.RxObserverHelper.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.User;
import com.soundcloud.android.properties.ApplicationProperties;
import eu.inmite.android.lib.dialogs.BaseDialogFragment;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class CreatePlaylistDialogFragment extends BaseDialogFragment {

    private static final String KEY_ORIGIN_SCREEN = "ORIGIN_SCREEN";
    private static final String KEY_TRACK_ID = "TRACK_ID";

    private EventBus mEventBus;
    private ApplicationProperties mApplicationProperties;

    public static CreatePlaylistDialogFragment from(long trackId, String originScreen) {
        Bundle b = new Bundle();
        b.putLong(KEY_TRACK_ID, trackId);
        b.putString(KEY_ORIGIN_SCREEN, originScreen);
        CreatePlaylistDialogFragment fragment = new CreatePlaylistDialogFragment();
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mEventBus = ((SoundCloudApplication) activity.getApplication()).getEventBus();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplicationProperties = new ApplicationProperties(getResources());
    }

    @Override
    protected Builder build(Builder initialBuilder) {
        final View dialogView = View.inflate(getActivity(), R.layout.dialog_create_new_playlist, null);
        final EditText input = (EditText) dialogView.findViewById(android.R.id.edit);
        final CheckBox privacy = (CheckBox) dialogView.findViewById(R.id.chk_private);

        initialBuilder.setTitle(R.string.create_new_playlist);
        initialBuilder.setView(dialogView);

        if (!mApplicationProperties.isDevBuildRunningOnDalvik()){
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
                    createPlaylist(playlistTitle, mApplicationProperties.isDevBuildRunningOnDalvik() && privacy.isChecked());
                    getDialog().dismiss();
                }
            }
        });
        return initialBuilder;
    }

    private void createPlaylist(final String title, final boolean isPrivate) {
        PlaylistOperations playlistOperations = new PlaylistOperations(getActivity());
        final User currentUser = SoundCloudApplication.instance.getLoggedInUser();
        final long firstTrackId = getArguments().getLong(KEY_TRACK_ID);
        final String originScreen = getArguments().getString(KEY_ORIGIN_SCREEN);
        fireAndForget(playlistOperations.createNewPlaylist(currentUser, title, isPrivate, firstTrackId));
        mEventBus.publish(EventQueue.UI, UIEvent.fromAddToPlaylist(originScreen, true, firstTrackId));
    }
}
