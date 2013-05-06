package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.fragment.FriendFinderFragment;

import android.content.Context;
import android.view.View;

public class FriendFinderEmptyView extends EmptyListView {

    public FriendFinderEmptyView(Context context) {
        super(context);
    }

    @Override
    public boolean setStatus(int mode) {

        if (mMode != mode) {
            mMode = mode;
            switch (mode) {
                case FriendFinderFragment.Status.NO_CONNECTIONS:
                    mProgressBar.setVisibility(View.GONE);
                    showEmptyLayout();
                    setMessageText(R.string.list_empty_friend_finder_no_connections);
                    mBtnAction.setVisibility(View.VISIBLE);
                    return true;

                case FriendFinderFragment.Status.CONNECTION_ERROR:
                    mProgressBar.setVisibility(View.GONE);
                    showEmptyLayout();
                    setMessageText(R.string.list_empty_friend_finder_error);
                    mBtnAction.setVisibility(View.GONE);
                    return true;

                case Status.WAITING:
                    mProgressBar.setVisibility(View.VISIBLE);
                    if (mEmptyLayout != null) mEmptyLayout.setVisibility(View.GONE);
                    return true;

                default:
                    mProgressBar.setVisibility(View.GONE);
                    showEmptyLayout();
                    setMessageText(R.string.list_empty_friend_finder);
                    return true;
            }
        }
        return false;
    }

    @Override
    protected int getEmptyViewLayoutId() {
        return R.layout.friend_finder_empty_collection_view;
    }
}
