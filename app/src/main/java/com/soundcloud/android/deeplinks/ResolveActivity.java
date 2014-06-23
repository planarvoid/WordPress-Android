package com.soundcloud.android.deeplinks;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.main.LauncherActivity;
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
import android.widget.Toast;

import javax.inject.Inject;

public class ResolveActivity extends TrackedActivity implements FetchModelTask.Listener<ScResource> {

    @Nullable
    private ResolveFetchTask resolveTask;
    private PublicCloudAPI oldCloudAPI;
    @Inject AccountOperations accountOperations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resolve);
        oldCloudAPI = new PublicApi(this);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (accountOperations.isUserLoggedIn()) {
            Intent intent = getIntent();
            Uri data = intent.getData();

            final boolean shouldResolve = data != null &&
                    (Intent.ACTION_VIEW.equals(intent.getAction()) || FacebookSSOActivity.handleFacebookView(this, intent));

            if (shouldResolve) {
                fetchData(data);
            } else {
                finish();
            }
        } else {
            showLaunchActivity();
        }
    }

    private void fetchData(Uri data) {
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        resolveTask = new ResolveFetchTask(oldCloudAPI);
        resolveTask.setListener(this);
        resolveTask.execute(data);
    }

    private void showLaunchActivity() {
        Toast.makeText(this, getString(R.string.error_toast_user_not_logged_in), Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LauncherActivity.class));
        finish();
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


