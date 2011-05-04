package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.util.Log;

import java.util.List;

public class LoadFriendsTask extends LoadJsonTask<Connection> {
    public LoadFriendsTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected List<Connection> doInBackground(Request... path) {
        Log.i("UserBrowser","Loading MY FRIENDS");
        return list(Request.to(Endpoints.MY_FRIENDS), Connection.class);
    }
}
