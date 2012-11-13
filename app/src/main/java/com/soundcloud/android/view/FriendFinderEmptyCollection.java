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
    }

    @Override
    public boolean setMode(int mode) {
        if (!super.setMode(mode)) {
            switch (mode) {
                case FriendFinderMode.NO_CONNECTIONS:
                    mEmptyLayout.setVisibility(View.VISIBLE);
                    mSyncLayout.setVisibility(View.GONE);
                    findViewById(R.id.txt_sync).setVisibility(View.GONE);
                    setMessageText("NO CONNECTIONS");
                    return true;

                case FriendFinderMode.CONNECTION_ERROR:
                    mEmptyLayout.setVisibility(View.VISIBLE);
                    mSyncLayout.setVisibility(View.GONE);
                    findViewById(R.id.txt_sync).setVisibility(View.GONE);
                    setMessageText("ERROR GETTING CONNECTIONS");
                    return true;
            }
        }
        return false;
    }
}
