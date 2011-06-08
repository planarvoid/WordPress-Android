package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.view.FriendFinderView;
import com.soundcloud.api.Request;

public class FriendFinderEndlessAdapter extends LazyEndlessAdapter{

    private FriendFinderView mFriendFinderView;

    public FriendFinderEndlessAdapter(ScActivity activity, LazyBaseAdapter wrapped, Request request) {
        super(activity,wrapped,request);
    }

    public void setFriendFinderView(FriendFinderView friendFinderView){
        mFriendFinderView = friendFinderView;
    }

    @Override
    public void onPostTaskExecute(Boolean keepgoing) {
        super.onPostTaskExecute(keepgoing);
        if (keepgoing != null && !keepgoing && mFriendFinderView != null) {
            mFriendFinderView.setState(FriendFinderView.States.FB_CONNECTION_NO_FRIENDS, true);
        }
    }

}
