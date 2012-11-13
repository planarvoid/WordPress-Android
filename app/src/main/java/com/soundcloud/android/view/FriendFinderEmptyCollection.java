package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class FriendFinderEmptyCollection extends EmptyCollection {

    public interface FriendFinderMode {
        int NO_CONNECTIONS = 100;
        int CONNECTION_ERROR = 101;
    }

    public FriendFinderEmptyCollection(Context context) {
        super(context);

        mBtnAction.setBackgroundResource(R.drawable.btn_fb_bg_states);
        mBtnAction.setTextColor(getResources().getColorStateList(R.drawable.txt_btn_blue_states));
    }

    @Override
    public boolean setMode(int mode) {
        if (!super.setMode(mode)) {
            switch (mode) {
                case FriendFinderMode.NO_CONNECTIONS:
                    mEmptyLayout.setVisibility(View.VISIBLE);
                    mSyncLayout.setVisibility(View.GONE);
                    findViewById(R.id.txt_sync).setVisibility(View.GONE);
                    setMessageText(-1);
                    mBtnAction.setVisibility(View.VISIBLE);
                    setImageVisibility(false);
                    return true;

                case FriendFinderMode.CONNECTION_ERROR:
                    mEmptyLayout.setVisibility(View.VISIBLE);
                    mSyncLayout.setVisibility(View.GONE);
                    findViewById(R.id.txt_sync).setVisibility(View.GONE);
                    setMessageText(R.string.problem_getting_connections);
                    mBtnAction.setVisibility(View.GONE);
                    setImageVisibility(true);
                    return true;
            }
        } else {
            setImageVisibility(true);
        }
        return false;
    }
}
