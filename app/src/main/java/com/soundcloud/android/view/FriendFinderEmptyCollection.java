package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.view.View;

public class FriendFinderEmptyCollection extends EmptyListView {

    public interface FriendFinderMode {
        int NO_CONNECTIONS   = 100;
        int CONNECTION_ERROR = 101;
    }

    public FriendFinderEmptyCollection(Context context) {
        super(context);
    }

    @Override
    public boolean setMode(int mode) {
        if (!super.setMode(mode)) {
            switch (mode) {
                case FriendFinderMode.NO_CONNECTIONS:
                    mProgressBar.setVisibility(View.GONE);
                    showEmptyLayout();
                    setMessageText(-1);
                    mBtnAction.setVisibility(View.VISIBLE);
                    return true;

                case FriendFinderMode.CONNECTION_ERROR:
                    mProgressBar.setVisibility(View.GONE);
                    showEmptyLayout();
                    setMessageText(R.string.problem_getting_connections);
                    mBtnAction.setVisibility(View.GONE);
                    return true;
            }
        }
        return false;
    }

    @Override
    protected void showEmptyLayout() {
        super.showEmptyLayout();

        mBtnAction.setBackgroundResource(R.drawable.next_button_blue);
        mBtnAction.setTextColor(getResources().getColorStateList(R.drawable.txt_btn_blue_states));
        final float density = getContext().getResources().getDisplayMetrics().density;

        // padding resets after you set a background in code
        mBtnAction.setPadding((int) (20 * density), (int) (5 * density), (int) (20 * density), (int) (5 * density));
    }
}
