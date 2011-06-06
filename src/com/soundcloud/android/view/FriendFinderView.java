package com.soundcloud.android.view;

import android.nfc.Tag;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.objects.Connection.Service;
import com.soundcloud.android.objects.Friend;
import com.soundcloud.android.objects.User;
import com.soundcloud.api.Endpoints;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.List;

public class FriendFinderView extends ScTabView {

    private RelativeLayout mLoadingLayout;
    private RelativeLayout mSuggestedLayout;

    public LazyListView friendList;

    public FriendFinderView(ScActivity activity, LazyEndlessAdapter adpWrap) {
        super(activity, adpWrap);

        LayoutInflater inflater = activity.getLayoutInflater();
        mLoadingLayout = (RelativeLayout)inflater.inflate(R.layout.loading_fill, null);
        mLoadingLayout.setVisibility(View.GONE);
        addView(mLoadingLayout);

        mSuggestedLayout = (RelativeLayout)inflater.inflate(R.layout.suggested_users, null);
        mSuggestedLayout.findViewById(R.id.facebook_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //facebook connect
            }
        });
    }

    public void showLoading() {
        mLoadingLayout.setVisibility(View.VISIBLE);
        friendList.setVisibility(View.GONE);
        mSuggestedLayout.setVisibility(View.GONE);
    }

    public void showList(List<Connection> connections, boolean refresh) {

        if (Connection.checkConnectionListForService(connections, Service.Facebook)){
            friendList.getWrapper().setPath(Endpoints.MY_FRIENDS, null);
            ((LazyBaseAdapter) friendList.getAdapter()).setModel(Friend.class);
            if (friendList.getParent() == mSuggestedLayout.findViewById(R.id.listHolder)){
                ((FrameLayout) mSuggestedLayout.findViewById(R.id.listHolder)).removeView(friendList);
                addView(friendList);
            }
        } else {
            friendList.getWrapper().setPath(Endpoints.SUGGESTED_USERS, null);
            ((LazyBaseAdapter) friendList.getAdapter()).setModel(User.class);
            if (friendList.getParent() == this){
                removeView(friendList);
                ((FrameLayout) mSuggestedLayout.findViewById(R.id.listHolder)).addView(friendList);
            }
            mSuggestedLayout.setVisibility(View.VISIBLE);
        }

        mLoadingLayout.setVisibility(View.GONE);
        friendList.setVisibility(View.VISIBLE);
        if (refresh) friendList.getWrapper().refresh(false);
    }

}
