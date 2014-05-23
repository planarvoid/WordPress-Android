package com.soundcloud.android.deeplinks;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.main.TrackedActivity;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.onboarding.auth.FacebookSSOActivity;
import com.soundcloud.android.tasks.FetchModelTask;
import com.soundcloud.android.utils.AndroidUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class ResolveActivity extends TrackedActivity implements FetchModelTask.Listener<ScResource> {

    @Nullable
    private ResolveFetchTask resolveTask;
    private PublicCloudAPI oldCloudAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resolve);
        oldCloudAPI = new PublicApi(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        Uri data = intent.getData();

        final boolean shouldResolve = data != null &&
                (Intent.ACTION_VIEW.equals(intent.getAction()) || FacebookSSOActivity.handleFacebookView(this, intent));

        if (shouldResolve) {
            findViewById(R.id.progress).setVisibility(View.VISIBLE);
            resolveTask = new ResolveFetchTask(oldCloudAPI);
            resolveTask.setListener(this);
            resolveTask.execute(data);
        } else {
            finish();
        }
    }

    @Override
    public void onError(Object context) {
        resolveTask = null;
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

        resolveTask = null;
        Intent intent = resource.getViewIntent();
        if (intent != null){
            Screen.DEEPLINK.addToIntent(intent);
            startActivity(intent);
        } else {
            Log.e(SoundCloudApplication.TAG,"Cannot find view intent for resource " + resource);
        }

        finish();
    }
}


