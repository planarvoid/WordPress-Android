package com.soundcloud.android.playlists;

import com.soundcloud.android.utils.CallsiteToken;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.ReactiveAdapter;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;

public class PlaylistLikesAdapter extends ItemAdapter<PropertySet>
        implements ReactiveAdapter<Iterable<PropertySet>> {

    private final CallsiteToken callsiteToken = CallsiteToken.build();

    @Inject
    public PlaylistLikesAdapter(PlaylistItemPresenter playlistPresenter) {
        super(playlistPresenter);
    }

    @Override
    public void onNext(Iterable<PropertySet> propertySets) {
        for (PropertySet propertySet : propertySets) {
            addItem(propertySet);
        }
        notifyDataSetChanged();
    }

    @Override
    public void onCompleted() {
        // no-op
    }

    @Override
    public void onError(Throwable e) {
        ErrorUtils.handleThrowable(e, callsiteToken);
    }
}
