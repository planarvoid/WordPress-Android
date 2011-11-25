package com.soundcloud.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.*;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.ArrayList;

public class TestActivity extends ScActivity {

    private ScListView mListView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        FrameLayout frameLayout = new FrameLayout(this);
        setContentView(frameLayout);

        TracklistAdapter adp = new TracklistAdapter(this, new ArrayList<Parcelable>(), Track.class);
        LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.MY_FAVORITES), ScContentProvider.Content.ME_FAVORITES, true);

        mListView = buildList();
        mListView.setAdapter(adpWrap);
        frameLayout.addView(mListView);

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            mListView.getWrapper().restoreState(mPreviousState);
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mListView != null) {
            return mListView.getWrapper().saveState();
        }
        return null;
    }


}
