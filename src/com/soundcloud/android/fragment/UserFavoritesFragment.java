package com.soundcloud.android.fragment;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.loader.ApiCollectionLoader;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.os.Parcelable;

import java.util.ArrayList;

public class UserFavoritesFragment extends RemoteListFragment {

    @Override
    protected LazyEndlessAdapter newAdapter() {
        return new LazyEndlessAdapter((ScActivity) getActivity(),
                new TracklistAdapter((ScActivity) getActivity(),
                        new ArrayList<Parcelable>(), Track.class), Request.to(Endpoints.MY_FAVORITES),
                true);
    }

    @Override
    protected void onListItemClick(int position) {
    }

    @Override
    void loadMore() {
    }
}
