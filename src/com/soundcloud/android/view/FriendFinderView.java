package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Connect;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.SectionedUserlistAdapter;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.Connection.Service;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.NewConnectionTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FriendFinderView extends ScTabView implements SectionedEndlessAdapter.SectionListener, ScListView.OnRefreshListener {
    private final RelativeLayout mHeaderLayout;
    private SectionedEndlessAdapter mAdapter;
    public ScListView mFriendList;
    private int mListAddPosition = -1;

    private int mCurrentState;
    private boolean mFbConnected;
    private SectionedAdapter.Section mFriendsSection;

    public interface States {
        int LOADING = 1;
        int NO_FB_CONNECTION = 2;
        int FB_CONNECTION = 3;
        int CONNECTION_ERROR = 4;
    }

    public FriendFinderView(ScActivity activity) {
        super(activity, null);

        LayoutInflater inflater = activity.getLayoutInflater();

        mHeaderLayout = (RelativeLayout) inflater.inflate(R.layout.suggested_users_header, null);
        mHeaderLayout.findViewById(R.id.facebook_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHeaderLayout.findViewById(R.id.facebook_btn).setEnabled(false);
                mHeaderLayout.findViewById(R.id.facebook_btn).getBackground().setAlpha(150);
                new NewConnectionTask(mActivity.getApp()) {
                @Override
                protected void onPostExecute(Uri uri) {
                    mHeaderLayout.findViewById(R.id.facebook_btn).setEnabled(true);
                    mHeaderLayout.findViewById(R.id.facebook_btn).getBackground().setAlpha(255);
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
        });

        mHeaderLayout.findViewById(R.id.suggested_users_msg_txt).setVisibility(View.GONE);
        ((TextView) mHeaderLayout.findViewById(R.id.suggested_users_msg_txt)).setText(R.string.suggested_users_no_friends_msg);
    }

    public void onConnections(List<Connection> connections, boolean refresh) {
        if (connections == null) {
            setState(States.CONNECTION_ERROR, refresh);
        } else {
            if (Connection.checkConnectionListForService(connections, Service.Facebook)) {
                setState(States.FB_CONNECTION, refresh);
            } else {
                setState(States.NO_FB_CONNECTION, refresh);
            }
        }
    }

    public void onSectionLoaded(SectionedAdapter.Section section) {
        if ((mFriendsSection == section && mFriendsSection.data.size() == 0 &&
            !mActivity.getApp().getAccountDataBoolean(User.DataKeys.FRIEND_FINDER_NO_FRIENDS_SHOWN))) {

            mActivity.showToast(R.string.suggested_users_no_friends_msg);
            mActivity.getApp().setAccountData(User.DataKeys.FRIEND_FINDER_NO_FRIENDS_SHOWN, true);
        }
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    public void setState(int state, boolean refresh) {
        switch (state) {
            case States.LOADING:
                if (mFriendList == null){
                    mFbConnected = true;
                    createList();
                }

                mFriendList.prepareForRefresh();
                mFriendList.setSelection(0);
                break;

            case States.CONNECTION_ERROR:
                if (mFriendList != null && !mFriendList.getWrapper().isEmpty()) {
                    mFriendList.onRefreshComplete(false);
                    mFriendList.postSelect(1, 0, true);
                } else if (mFriendList == null || mFbConnected) {
                    mFbConnected = false;
                    createList();
                }

                mFriendList.getWrapper().setEmptyViewText(mActivity.getString(R.string.error_loading_connections));
                mFriendList.getWrapper().applyEmptyText();
                mFriendList.getWrapper().reset(false, true);
                break;

            case States.NO_FB_CONNECTION:
                if (mFriendList == null || mFbConnected){
                    mFbConnected = false;
                    createList();
                }

                mFriendList.getWrapper().configureViews(mFriendList);
                if (refresh){
                    mFriendList.getWrapper().onRefresh();
                    mFriendList.prepareForRefresh();
                }
                mFriendList.setVisibility(View.VISIBLE);
                break;


            case States.FB_CONNECTION:
                if (mFriendList == null || !mFbConnected){
                    mFbConnected = true;
                    createList();
                }
                mFriendList.getWrapper().configureViews(mFriendList);
                if (refresh){
                    mFriendList.getWrapper().onRefresh();
                    mFriendList.prepareForRefresh();
                }
                mFriendList.setVisibility(View.VISIBLE);
                break;

            default:
                throw new IllegalArgumentException(("Improper setState parameter"));
        }

        mCurrentState = state;

    }



    private void removeList() {
        //mAdapter.clearEmptyView();
        mListAddPosition = mActivity.removeList(mFriendList);
        if (mFriendList.getParent() == this) removeView(mFriendList);
        mFriendList = null;
    }

    private void createList(){
        if (mFriendList != null) removeList();
        mFriendList = mActivity.configureList(new SectionedListView(mActivity), false, mListAddPosition);
        mFriendList.setOnRefreshListener(this);

        mAdapter = new SectionedEndlessAdapter(mActivity, new SectionedUserlistAdapter(mActivity));
        mAdapter.addListener(this);

        if (!mFbConnected) mFriendList.addHeaderView(mHeaderLayout);

        setLazyListView(mFriendList, mAdapter,
            Consts.ListId.LIST_USER_SUGGESTED, false).disableLongClickListener();

        if (mFbConnected) {
            addFriendsSection();
            addSuggestedSection();
            mFriendList.getWrapper().setRequest(Request.to(Endpoints.MY_FRIENDS));
            mFriendList.getBaseAdapter().setModel(Friend.class);

        } else {
            addSuggestedSection();
            mFriendList.getWrapper().setRequest(Request.to(Endpoints.SUGGESTED_USERS));
            mFriendList.getBaseAdapter().setModel(User.class);

        }
    }

    private void addFriendsSection() {
        mFriendsSection = new SectionedAdapter.Section(mActivity.getString(R.string.list_header_fb_friends),
                Friend.class, new ArrayList<Parcelable>(), Request.to(Endpoints.MY_FRIENDS));
        mAdapter.addSection(mFriendsSection);
    }

    private void addSuggestedSection() {
        mAdapter.getWrappedAdapter().sections.add(
                new SectionedAdapter.Section(mActivity.getString(R.string.list_header_suggested_users),
                        User.class, new ArrayList<Parcelable>(), Request.to(Endpoints.SUGGESTED_USERS)));
    }

    @Override
    public void onRefresh() {
        ((UserBrowser) mActivity).refreshConnections();
    }

    @Override
    public void onConnected() {
    }

    @Override
    public boolean needsRefresh() {
        return (mCurrentState == States.CONNECTION_ERROR);
    }

    @Override
    public boolean isRefreshing() {
        return (mCurrentState == States.LOADING);
    }

}
