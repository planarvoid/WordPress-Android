package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.FacebookSSO;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.task.fetch.ResolveFetchTask;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class Resolve extends Activity implements FetchModelTask.FetchModelListener<ScResource> {

    @Nullable
    private ResolveFetchTask mResolveTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.resolve);

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data == null || (!Intent.ACTION_VIEW.equals(intent.getAction()) && !FacebookSSO.handleFacebookView(this, intent))) {
            finish(); // nothing to do
        } else {
            mResolveTask = new ResolveFetchTask(getApp());
            mResolveTask.setListener(this);
            mResolveTask.execute(data);
        }
    }


    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    protected void onTrackLoaded(Track track, @Nullable String action) {
        mResolveTask = null;
        startService(track.getPlayIntent());
        startActivity(new Intent(this, ScPlayer.class));
    }

    protected void onUserLoaded(User u, @Nullable String action) {
        mResolveTask = null;
        startActivity(new Intent(this, UserBrowser.class)
            .putExtra("user", u)
            .putExtra("updateInfo", false));
    }

    @Override
    public void onError(long modelId) {
        mResolveTask = null;
        Toast.makeText(this, R.string.error_loading_url, Toast.LENGTH_LONG).show();
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


