package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Request;

import java.util.List;

public class LoadFollowingsTask extends LoadJsonTask<Request, Long> {
    public LoadFollowingsTask(AndroidCloudAPI app) {
        super(app);
    }

    @Override
    protected List<Long> doInBackground(Request... path) {
        return list(path[0], Long.class);
    }
}