package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.FacebookSSO;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.task.fetch.ResolveFetchTask;
import com.soundcloud.android.utils.AndroidUtils;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class Resolve extends Activity implements FetchModelTask.FetchModelListener<ScResource> {
    @Nullable
    private ResolveFetchTask mResolveTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resolve);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        Uri data = intent.getData();

        final boolean shouldResolve = data != null &&
                (Intent.ACTION_VIEW.equals(intent.getAction()) || FacebookSSO.handleFacebookView(this, intent));

        if (shouldResolve) {
            findViewById(R.id.progress).setVisibility(View.VISIBLE);
            mResolveTask = new ResolveFetchTask(getApp());
            mResolveTask.setListener(this);
            mResolveTask.execute(data);

        } else {
            finish();
        }
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    protected void onTrackLoaded(Track track, @Nullable String action) {
        mResolveTask = null;
        startActivity(track.getPlayIntent());
    }

    protected void onUserLoaded(User user, @Nullable String action) {
        mResolveTask = null;
        startActivity(user.getViewIntent());
    }

    @Override
    public void onError(long modelId) {
        mResolveTask = null;
        AndroidUtils.showToast(this, R.string.error_loading_url);
        finish();
    }

    @Override
    public void onSuccess(ScResource m, @Nullable String action) {
        mResolveTask = null;
        if (m instanceof Track) {
            onTrackLoaded((Track) m, null);
        } else if (m instanceof User) {
            onUserLoaded((User) m, null);
        }
    }
}


