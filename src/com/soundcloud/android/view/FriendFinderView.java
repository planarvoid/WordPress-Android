package com.soundcloud.android.view;

import android.view.Gravity;
import android.view.ViewGroup;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Connect;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.FriendFinderAdapter;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.Connection.Service;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.NewConnectionTask;
import com.soundcloud.android.utils.CloudUtils;
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

public class FriendFinderView extends ScTabView implements SectionedEndlessAdapter.SectionListener {

    private final RelativeLayout mLoadingLayout;
    private final RelativeLayout mHeaderLayout;

    private TextView mConnectionErrorTxt;
    private SectionedEndlessAdapter mAdapter;
    public LazyListView mFriendList;
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

        mLoadingLayout = (RelativeLayout) inflater.inflate(R.layout.loading_fill, null);
        addView(mLoadingLayout);
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
        if (refresh && mAdapter != null) mAdapter.clearData();

        switch (state) {
            case States.LOADING:
                if (mFriendList != null) removeList();
                if (mConnectionErrorTxt != null) mConnectionErrorTxt.setVisibility(View.GONE);
                mLoadingLayout.setVisibility(View.VISIBLE);
                break;

            case States.CONNECTION_ERROR:
                if (mFriendList != null) {
                    mFriendList.getWrapper().clearEmptyView();
                    mFriendList.setVisibility(View.GONE);
                }

                if (mConnectionErrorTxt == null){
                    mConnectionErrorTxt = createConnectionErrorText();
                    addView(mConnectionErrorTxt);
                }
                mConnectionErrorTxt.setVisibility(View.VISIBLE);
                mLoadingLayout.setVisibility(View.GONE);
                break;

            case States.NO_FB_CONNECTION:
                if (mFriendList == null || mFbConnected){
                    mFbConnected = false;
                    createList();
                }

                if (refresh) addSuggestedSection();
                mFriendList.getWrapper().setRequest(Request.to(Endpoints.SUGGESTED_USERS));
                ((LazyBaseAdapter) mFriendList.getAdapter()).setModel(User.class);
                showList(refresh);
                break;


            case States.FB_CONNECTION:
                if (mFriendList == null || !mFbConnected){
                    mFbConnected = true;
                    createList();
                }

                if (refresh) {
                    addFriendsSection();
                    addSuggestedSection();
                }
                mFriendList.getWrapper().setRequest(Request.to(Endpoints.MY_FRIENDS));
                ((LazyBaseAdapter) mFriendList.getAdapter()).setModel(Friend.class);
                showList(refresh);
                break;

            default:
                throw new IllegalArgumentException(("Improper setState parameter"));
        }
        mCurrentState = state;
    }

    private void showList(boolean refresh) {
        mLoadingLayout.setVisibility(View.GONE);
        if (mConnectionErrorTxt != null) mConnectionErrorTxt.setVisibility(View.GONE);
        mFriendList.getWrapper().createListEmptyView(mFriendList);
        mFriendList.setVisibility(View.VISIBLE);

        if (refresh) {
            mFriendList.getWrapper().refresh(false);
            mFriendList.invalidate();
            mFriendList.requestLayout();
        }
    }

    private TextView createConnectionErrorText() {
        TextView errorView = new TextView(mActivity);
        errorView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        errorView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        errorView.setVisibility(View.GONE);
        errorView.setPadding(5, 5, 5, 5);
        errorView.setTextAppearance(mActivity, R.style.txt_empty_view);
        errorView.setText(R.string.error_loading_connections);
        return errorView;
    }

    private void removeList() {
        mAdapter.clearEmptyView();
        mListAddPosition = mActivity.removeList(mFriendList);
        if (mFriendList.getParent() == this) removeView(mFriendList);
        mFriendList = null;
    }

    private void createList(){
        if (mFriendList != null) removeList();
        mFriendList = mActivity.configureList(new SectionedListView(mActivity), mListAddPosition);
        mAdapter = new SectionedEndlessAdapter(mActivity, new FriendFinderAdapter(mActivity));
        mAdapter.addListener(this);

        if (!mFbConnected) mFriendList.addHeaderView(mHeaderLayout);

        CloudUtils.configureTabList(mActivity, mFriendList, this, mAdapter,
            CloudUtils.ListId.LIST_USER_SUGGESTED, null).disableLongClickListener();
    }

    private void addFriendsSection() {
        mFriendsSection = new SectionedAdapter.Section(mActivity.getString(R.string.list_header_fb_friends),
                Friend.class, new ArrayList<Parcelable>(), Request.to(Endpoints.MY_FRIENDS));
        ((SectionedAdapter) mAdapter.getWrappedAdapter()).sections.add(mFriendsSection);
    }

    private void addSuggestedSection() {
        ((SectionedAdapter) mAdapter.getWrappedAdapter()).sections.add(
                new SectionedAdapter.Section(mActivity.getString(R.string.list_header_suggested_users),
                        User.class, new ArrayList<Parcelable>(), Request.to(Endpoints.SUGGESTED_USERS)));
    }
}
