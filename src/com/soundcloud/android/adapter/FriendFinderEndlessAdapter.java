package com.soundcloud.android.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.FriendFinderView;
import com.soundcloud.api.Request;
import org.w3c.dom.UserDataHandler;

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
        if (keepgoing != null && !keepgoing && mFriendFinderView != null && getWrappedAdapter().getCount() == 0) {
            mFriendFinderView.setState(FriendFinderView.States.FB_CONNECTION_NO_FRIENDS, true);
            if (!mActivity.getSoundCloudApplication().getAccountDataBoolean(User.DataKeys.FRIEND_FINDER_NO_FRIENDS_SHOWN)){
                mActivity.showToast(R.string.suggested_users_no_friends_msg);
                mActivity.getSoundCloudApplication().setAccountData(User.DataKeys.FRIEND_FINDER_NO_FRIENDS_SHOWN,true);
            }

        }
    }

}
