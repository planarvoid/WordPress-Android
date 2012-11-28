package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

public class FriendFinderEmptyCollection extends EmptyCollection {

    public interface FriendFinderMode {
        int NO_CONNECTIONS = 100;
        int CONNECTION_ERROR = 101;
    }

    public FriendFinderEmptyCollection(Context context) {
        super(context);

        mBtnAction.setBackgroundResource(R.drawable.next_button_blue);
        mBtnAction.setTextColor(getResources().getColorStateList(R.drawable.txt_btn_blue_states));

        final float density = context.getResources().getDisplayMetrics().density;

        // padding resets after you set a background in code
        mBtnAction.setPadding((int) (20 * density),(int) (5 * density),(int) (20 * density),(int) (5 * density));
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
