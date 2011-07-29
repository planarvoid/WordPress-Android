package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.adapter.SectionedUserlistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.NewConnectionTask;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.android.view.TrackInfoBar;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class TrackFavoriters extends ScActivity implements SectionedEndlessAdapter.SectionListener {
    private ScListView mListView;
    private SectionedAdapter.Section mFriendsSection;
    private SectionedUserlistAdapter userAdapter;
    private SectionedEndlessAdapter userAdapterWrapper;
    private Track mTrack;
    private Button facebookBtn;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.track_favoriters);

        Intent i = getIntent();
        if (!i.hasExtra("track")) throw new IllegalArgumentException("No track supplied with intent");
        mTrack = i.getParcelableExtra("track");

        ((TrackInfoBar) findViewById(R.id.track_info_bar)).display(mTrack, true, -1);

        userAdapter = new SectionedUserlistAdapter(this);
        userAdapterWrapper = new SectionedEndlessAdapter(this, userAdapter, true);
        userAdapterWrapper.addListener(this);

        mListView = new SectionedListView(this);
        configureList(mListView);
        ((ViewGroup) findViewById(R.id.listHolder)).addView(mListView);

        userAdapterWrapper.configureViews(mListView);
        userAdapterWrapper.setEmptyViewText(getResources().getString(R.string.empty_list));
        mListView.setAdapter(userAdapterWrapper,true);

        //TODO real endpoint
        userAdapter.sections.add(
                new SectionedAdapter.Section(getString(R.string.list_header_track_favoriters),
                        User.class, new ArrayList<Parcelable>(), Request.to("/tracks/%d/favoriters", mTrack.id)));
    }

    @Override
    public void onResume() {
        super.onResume();
        pageTrack("/track_favoriters");
    }

    @Override
    public void onSectionLoaded(SectionedAdapter.Section section) {
    }
}
