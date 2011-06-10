package com.soundcloud.android.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Connect;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.objects.Connection.Service;
import com.soundcloud.android.objects.Friend;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.NewConnectionTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.ArrayList;
import java.util.List;

public class FriendFinderView extends ScTabView implements SectionedEndlessAdapter.SectionListener {

    private final RelativeLayout mLoadingLayout;
    private final RelativeLayout mSuggestedLayout;
    private final SectionedEndlessAdapter mAdapter;

    private int mCurrentState;
    private SectionedAdapter.Section mFriendsSection;
    private boolean mHidingListLandscape;
    private TextView mTxtGoToPortrait;

    public LazyListView friendList;

    public interface States {
        int LOADING = 1;
        int NO_FB_CONNECTION = 2;
        int FB_CONNECTION = 3;
    }

    public FriendFinderView(ScActivity activity, SectionedEndlessAdapter adpWrap) {
        super(activity, adpWrap);

        mAdapter = adpWrap;
        mAdapter.addListener(this);

        LayoutInflater inflater = activity.getLayoutInflater();
        mLoadingLayout = (RelativeLayout) inflater.inflate(R.layout.loading_fill, null);
        addView(mLoadingLayout);

        mSuggestedLayout = (RelativeLayout) inflater.inflate(R.layout.friend_finder, null);
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

    public void onConnections(List<Connection> connections, boolean refresh) {
        if (connections == null) {
            /* cheap way of showing an error */
            setState(States.FB_CONNECTION, refresh);
        } else if (Connection.checkConnectionListForService(connections, Service.Facebook)) {
            setState(States.FB_CONNECTION, refresh);
        } else {
            setState(States.NO_FB_CONNECTION, refresh);
        }
    }

    public int getCurrentState() {
        return mCurrentState;
    }


    public void setState(int state, boolean refresh) {

        if (refresh) mAdapter.clearData();

        switch (state) {
            case States.LOADING:
                friendList.getWrapper().clearEmptyView();
                mLoadingLayout.setVisibility(View.VISIBLE);
                friendList.setVisibility(View.GONE);
                mSuggestedLayout.setVisibility(View.GONE);
                mCurrentState = state;
                return;

            case States.NO_FB_CONNECTION:
                if (refresh) addSuggestedSection();
                mSuggestedLayout.findViewById(R.id.facebook_btn).setEnabled(true);
                mSuggestedLayout.findViewById(R.id.facebook_btn).setVisibility(View.VISIBLE);
                showSuggestedList();
                break;

            case States.FB_CONNECTION:
                if (refresh) {
                    addFriendsSection();
                    addSuggestedSection();
                }
                showFriendsList();
                break;

            default:
                throw new IllegalArgumentException(("Improper setState parameter"));
        }


        mCurrentState = state;
        mLoadingLayout.setVisibility(View.GONE);
        friendList.getWrapper().createListEmptyView(friendList);
        if (!mHidingListLandscape) friendList.setVisibility(View.VISIBLE);

        if (refresh) {
            friendList.getWrapper().refresh(false);
            friendList.invalidate();
            friendList.requestLayout();
        }
    }

    private void addFriendsSection() {
        mFriendsSection = new SectionedAdapter.Section("Facebook Friends", Friend.class, new ArrayList<Parcelable>(), Request.to(Endpoints.MY_FRIENDS));
        ((SectionedAdapter) mAdapter.getWrappedAdapter()).sections.add(mFriendsSection);
    }

    private void addSuggestedSection() {
        ((SectionedAdapter) mAdapter.getWrappedAdapter()).sections.add(
                new SectionedAdapter.Section("Suggested Users", User.class, new ArrayList<Parcelable>(), Request.to(Endpoints.SUGGESTED_USERS)));
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

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed){
            if (CloudUtils.isLandscape(getResources()) && mCurrentState == States.NO_FB_CONNECTION){
                mHidingListLandscape = true;
                friendList.setVisibility(View.GONE);
                if (mTxtGoToPortrait == null) mTxtGoToPortrait = createGoToPortraitMessage();
                if (mTxtGoToPortrait.getParent() != mSuggestedLayout.findViewById(R.id.listHolder)) {
                    ((FrameLayout) mSuggestedLayout.findViewById(R.id.listHolder)).addView(mTxtGoToPortrait);
                }
                requestLayout();
            } else if (mHidingListLandscape){
                // layout has changed
                mHidingListLandscape = false;
                if (mCurrentState != States.LOADING) friendList.setVisibility(View.VISIBLE);

                if (mTxtGoToPortrait.getParent() == mSuggestedLayout.findViewById(R.id.listHolder)) {
                    ((FrameLayout) mSuggestedLayout.findViewById(R.id.listHolder)).removeView(mTxtGoToPortrait);
                }
                mTxtGoToPortrait = null;
                requestLayout();
            }
        }

    }

    private TextView createGoToPortraitMessage() {
        TextView tv = new TextView(mActivity);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        tv.setPadding(5, 5, 5, 5);
        tv.setTextAppearance(mActivity, R.style.txt_empty_view);
        tv.setText(R.string.view_suggested_users_in_portrait);
        return tv;
    }

    public void onSectionLoaded(SectionedAdapter.Section section) {
        if ((mFriendsSection == section && mFriendsSection.data.size() == 0 &&
                !mActivity.getSoundCloudApplication().getAccountDataBoolean(User.DataKeys.FRIEND_FINDER_NO_FRIENDS_SHOWN))){
            mActivity.showToast(R.string.suggested_users_no_friends_msg);
            mActivity.getSoundCloudApplication().setAccountData(User.DataKeys.FRIEND_FINDER_NO_FRIENDS_SHOWN, true);
        }
    }

}
