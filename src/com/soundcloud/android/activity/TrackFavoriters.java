package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.adapter.SectionedUserlistAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.task.LoadTrackInfoTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.android.view.TrackInfoBar;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class TrackFavoriters extends TrackInfoCollection {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    protected SectionedAdapter createSectionedAdapter() {
        return new SectionedUserlistAdapter(this);
    }

    @Override
    protected SectionedAdapter.Section createSection() {
        return new SectionedAdapter.Section(getString(R.string.list_header_track_favoriters),
                User.class, new ArrayList<Parcelable>(), Request.to(Endpoints.TRACK_FAVORITERS, mTrack.id), null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTrack != null) {
            trackPage(mTrack.pageTrack("favorites"));
        }
    }


}
