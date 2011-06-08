package com.soundcloud.android.view;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Connect;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.FriendFinderEndlessAdapter;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.objects.Connection.Service;
import com.soundcloud.android.objects.Friend;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.NewConnectionTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.List;

public class FriendFinderView extends ScTabView {

    private RelativeLayout mLoadingLayout;
    private RelativeLayout mSuggestedLayout;
    private int mCurrentState;

    public LazyListView friendList;

    public interface States {
        int LOADING = 1;
        int NO_FB_CONNECTION = 2;
        int FB_CONNECTION  = 3;
        int FB_CONNECTION_NO_FRIENDS  = 4;
    }

    public FriendFinderView(ScActivity activity, FriendFinderEndlessAdapter adpWrap) {
        super(activity, adpWrap);

        adpWrap.setFriendFinderView(this);

        LayoutInflater inflater = activity.getLayoutInflater();
        mLoadingLayout = (RelativeLayout) inflater.inflate(R.layout.loading_fill, null);
        addView(mLoadingLayout);

        mSuggestedLayout = (RelativeLayout) inflater.inflate(R.layout.suggested_users, null);
        ((TextView) mSuggestedLayout.findViewById(R.id.suggested_users_msg))
                    .setText(R.string.suggested_users_no_friends_msg);
        mSuggestedLayout.findViewById(R.id.facebook_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSuggestedLayout.findViewById(R.id.facebook_btn).setEnabled(false);
                configureFacebook();
            }
        });
        mSuggestedLayout.findViewById(R.id.listTitle).setVisibility(View.GONE);
        mSuggestedLayout.setVisibility(View.GONE);
        addView(mSuggestedLayout);
    }

    public void onConnections(List<Connection> connections, boolean refresh) {
        if (connections == null /* cheap way of showing an error */
                || Connection.checkConnectionListForService(connections, Service.Facebook)) {
            setState(States.FB_CONNECTION, refresh);
        } else {
            setState(States.NO_FB_CONNECTION, refresh);
        }
    }

    public int getCurrentState(){
        return mCurrentState;
    }


    public void setState(int state, boolean refresh) {
        switch (state){
            case States.LOADING:
                mLoadingLayout.setVisibility(View.VISIBLE);
                friendList.setVisibility(View.GONE);
                mSuggestedLayout.setVisibility(View.GONE);
                mCurrentState = state;
                return;

            case States.NO_FB_CONNECTION :
                showSuggestedList();
                mSuggestedLayout.findViewById(R.id.facebook_btn).setEnabled(true);
                mSuggestedLayout.findViewById(R.id.facebook_btn).setVisibility(View.VISIBLE);
                mSuggestedLayout.findViewById(R.id.suggested_users_msg).setVisibility(View.GONE);
                break;

            case States.FB_CONNECTION :
                showFriendsList();
                break;

            case States.FB_CONNECTION_NO_FRIENDS :
                showSuggestedList();
                mSuggestedLayout.findViewById(R.id.facebook_btn).setVisibility(View.GONE);
                mSuggestedLayout.findViewById(R.id.suggested_users_msg).setVisibility(View.VISIBLE);
                break;

            default:
                throw new IllegalArgumentException(("Improper setState parameter"));
        }


        mCurrentState = state;
        friendList.getWrapper().createListEmptyView(friendList);
        mLoadingLayout.setVisibility(View.GONE);
        friendList.setVisibility(View.VISIBLE);
        if (refresh) {
            friendList.getWrapper().refresh(false);
            friendList.invalidate();
            friendList.requestLayout();
        }
    }

    private void showFriendsList() {
        friendList.getWrapper().setRequest(Request.to(Endpoints.MY_FRIENDS));
        ((LazyBaseAdapter) friendList.getAdapter()).setModel(Friend.class);

        if (friendList.getParent() == mSuggestedLayout.findViewById(R.id.listHolder)) {
            ((FrameLayout) mSuggestedLayout.findViewById(R.id.listHolder)).removeView(friendList);
        }
        if (friendList.getParent() != this) addView(friendList);

    }

    private void showSuggestedList() {
        friendList.getWrapper().setRequest(Request.to(Endpoints.SUGGESTED_USERS));
        ((LazyBaseAdapter) friendList.getAdapter()).setModel(User.class);

        if (friendList.getParent() == this) {
            removeView(friendList);
        }
        if (friendList.getParent() != ((FrameLayout) mSuggestedLayout.findViewById(R.id.listHolder))) {
            ((FrameLayout) mSuggestedLayout.findViewById(R.id.listHolder)).addView(friendList);
        }

        mSuggestedLayout.setVisibility(View.VISIBLE);
    }


    public void configureFacebook() {
        new NewConnectionTask(mActivity.getSoundCloudApplication()) {
            @Override
            protected void onPostExecute(Uri uri) {
                if (uri != null) {
                    mActivity.startActivityForResult(
                            (new Intent(mActivity, Connect.class))
                                    .putExtra("service", Service.Facebook.name())
                                    .setData(uri),
                            Connect.MAKE_CONNECTION);
                } else {
                    mActivity.showToast(R.string.new_connection_error);
                }
            }
        }.execute(Service.Facebook);
    }

}
