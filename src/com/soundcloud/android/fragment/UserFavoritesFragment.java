package com.soundcloud.android.fragment;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.loader.ApiCollectionLoader;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

public class UserFavoritesFragment extends RemoteListFragment {

    @Override
    protected ApiCollectionLoader newLoader() {
        return new ApiCollectionLoader(SoundCloudApplication.fromContext(getActivity()),
                Request.to(Endpoints.MY_FAVORITES).add("linked_partitioning", "1"),
                Track.class);
    }

    @Override
    protected LazyBaseAdapter newAdapter() {
        return new TracklistAdapter(getActivity(),null,Track.class);
    }

    @Override
    protected void onListItemClick(int position) {
    }
}
