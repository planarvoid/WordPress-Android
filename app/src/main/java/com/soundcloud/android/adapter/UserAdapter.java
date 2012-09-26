package com.soundcloud.android.adapter;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.UserlistRow;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class UserAdapter extends ScBaseAdapter<User> {
    public UserAdapter(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected LazyRow createRow(int position) {
        return new UserlistRow(mContext, this);
    }

    @Override
    public void handleListItemClick(int position, long id) {
        Log.i(SoundCloudApplication.TAG, "Clicked on item " + id);
    }
}
