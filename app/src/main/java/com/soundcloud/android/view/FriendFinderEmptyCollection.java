package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.view.View;

public class FriendFinderEmptyCollection extends EmptyListView {

    public interface Status extends EmptyListView.Status {
        int NO_CONNECTIONS   = 1000;
        int CONNECTION_ERROR = 1001;
    }

    public FriendFinderEmptyCollection(Context context) {
        super(context);
    }

    @Override
    public boolean setStatus(int mode) {

        if (mMode != mode) {
            mMode = mode;
            switch (mode) {
                case Status.NO_CONNECTIONS:
                    mProgressBar.setVisibility(View.GONE);
                    showEmptyLayout();
                    setMessageText(R.string.list_empty_friend_finder_no_connections);
                    mBtnAction.setVisibility(View.VISIBLE);
                    return true;

                case Status.CONNECTION_ERROR:
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
