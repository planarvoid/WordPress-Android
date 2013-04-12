package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.FacebookSSO;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.task.fetch.ResolveFetchTask;
import com.soundcloud.android.utils.AndroidUtils;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class ResolveActivity extends Activity implements FetchModelTask.Listener<ScResource> {
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

    @Override
    public void onError(Object context) {
        mResolveTask = null;
        if (context instanceof Uri) {
            Uri unresolved = (Uri) context;
            // resolved to a soundcloud.com url ?
            if ("http".equals(unresolved.getScheme()) || "https".equals(unresolved.getScheme())) {
                startActivity(new Intent(this, WebViewActivity.class).setData(unresolved));
            } else {
                AndroidUtils.showToast(this, R.string.error_loading_url);
            }
        } else {
            AndroidUtils.showToast(this, R.string.error_loading_url);
        }
        finish();
    }

    @Override
    public void onSuccess(ScResource resource) {

        mResolveTask = null;
        Intent intent = resource.getViewIntent();
        if (intent != null){
            startActivity(intent);
        } else {
            Log.e(SoundCloudApplication.TAG,"Cannof find view intent for resource " + resource);
        }

        finish();
    }
}


