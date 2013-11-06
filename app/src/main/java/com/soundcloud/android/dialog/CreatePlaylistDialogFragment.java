package com.soundcloud.android.dialog;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.dao.PlaylistStorage;
import com.soundcloud.android.dao.SoundAssociationStorage;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.User;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.service.sync.SyncStateManager;
import rx.Observable;
import rx.util.functions.Func1;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class CreatePlaylistDialogFragment extends PlaylistDialogFragment {

    private AccountOperations mAccountOpertations;
    private ApplicationProperties mApplicationProperties;

    public static CreatePlaylistDialogFragment from(long trackId) {
        Bundle b = new Bundle();
        b.putLong(KEY_TRACK_ID, trackId);
        CreatePlaylistDialogFragment fragment = new CreatePlaylistDialogFragment();
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountOpertations = new AccountOperations(getActivity());
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
        final User loggedInUser = ((SoundCloudApplication) getActivity().getApplication()).getLoggedInUser();
        final Account account = mAccountOpertations.getSoundCloudAccount();

        PlaylistStorage playlistStorage = getPlaylistStorage();
        // insert the new playlist into the database
        playlistStorage.createNewUserPlaylistAsync(
                loggedInUser,
                title,
                isPrivate,
                getArguments().getLong(KEY_TRACK_ID)
        ).mapMany(new Func1<Playlist, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Playlist playlist) {
                // store the newly created playlist as a sound association
                final SoundAssociationStorage soundAssociationStorage = new SoundAssociationStorage();
                soundAssociationStorage.addCreation(playlist);
                // force to stale so we know to update the playlists next time it is viewed
                final SyncStateManager syncStateManager = new SyncStateManager(getActivity());
                return syncStateManager.forceToStaleAsync(Content.ME_PLAYLISTS);
            }
        }).subscribe(new DefaultObserver<Boolean>() {
            @Override
            public void onNext(Boolean success) {
                if (success) {
                    // request sync to push playlist at next possible opportunity
                    ContentResolver.requestSync(account, ScContentProvider.AUTHORITY, new Bundle());
                }
            }
        });
    }
}
