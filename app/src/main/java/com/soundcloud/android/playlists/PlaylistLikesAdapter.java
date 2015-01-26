package com.soundcloud.android.playlists;

import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.ReactiveAdapter;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;

public class PlaylistLikesAdapter extends EndlessAdapter<PropertySet>
        implements ReactiveAdapter<Iterable<PropertySet>> {

    @Inject
    public PlaylistLikesAdapter(PlaylistItemPresenter playlistPresenter) {
        super(playlistPresenter);
    }

}
