package com.soundcloud.android.view;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.Tag;
import android.view.Gravity;
import android.widget.Toast;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Connect;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.objects.Connection.Service;
import com.soundcloud.android.objects.Friend;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.NewConnectionTask;
import com.soundcloud.api.Endpoints;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.soundcloud.api.Request;

import java.util.List;

public class FriendFinderView extends ScTabView {

    private RelativeLayout mLoadingLayout;
    private RelativeLayout mSuggestedLayout;

    public LazyListView friendList;

    public FriendFinderView(ScActivity activity, LazyEndlessAdapter adpWrap) {
        super(activity, adpWrap);

        LayoutInflater inflater = activity.getLayoutInflater();
        mLoadingLayout = (RelativeLayout)inflater.inflate(R.layout.loading_fill, null);
        addView(mLoadingLayout);

        mSuggestedLayout = (RelativeLayout)inflater.inflate(R.layout.suggested_users, null);
        mSuggestedLayout.findViewById(R.id.suggested_users_msg).setVisibility(View.GONE);
        mSuggestedLayout.findViewById(R.id.facebook_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSuggestedLayout.findViewById(R.id.facebook_btn).setEnabled(false);
                configureFacebook();
            }
        });
        mSuggestedLayout.setVisibility(View.GONE);
        addView(mSuggestedLayout);
    }

    public void showLoading() {
        mSuggestedLayout.findViewById(R.id.facebook_btn).setEnabled(true);
        mLoadingLayout.setVisibility(View.VISIBLE);
        friendList.setVisibility(View.GONE);
        mSuggestedLayout.setVisibility(View.GONE);
    }

    public void showList(List<Connection> connections, boolean refresh) {
        if (connections == null /* cheap way of showing an error */
                || Connection.checkConnectionListForService(connections, Service.Facebook)){
            friendList.getWrapper().setRequest(Request.to(Endpoints.MY_FRIENDS));
            ((LazyBaseAdapter) friendList.getAdapter()).setModel(Friend.class);
            if (friendList.getParent() == mSuggestedLayout.findViewById(R.id.listHolder)){
                ((FrameLayout) mSuggestedLayout.findViewById(R.id.listHolder)).removeView(friendList);
                addView(friendList);
            }
        } else {
            friendList.getWrapper().setRequest(Request.to(Endpoints.SUGGESTED_USERS));
            ((LazyBaseAdapter) friendList.getAdapter()).setModel(User.class);
            if (friendList.getParent() == this){
                removeView(friendList);
                ((FrameLayout) mSuggestedLayout.findViewById(R.id.listHolder)).addView(friendList);
            }
            mSuggestedLayout.setVisibility(View.VISIBLE);
        }

        friendList.getWrapper().createListEmptyView(friendList);
        mLoadingLayout.setVisibility(View.GONE);
        friendList.setVisibility(View.VISIBLE);
        if (refresh) friendList.getWrapper().refresh(false);
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
