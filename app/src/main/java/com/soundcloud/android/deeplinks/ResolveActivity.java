package com.soundcloud.android.deeplinks;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;

import android.content.res.Resources;
import android.net.Uri;
import android.view.View;

import javax.inject.Inject;

public class ResolveActivity extends RootActivity {

    @Inject IntentResolver intentResolver;

    public ResolveActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    public static boolean accept(Uri data, Resources resources) {
        return DeepLink.SOUNDCLOUD_SCHEME.equalsIgnoreCase(data.getScheme())
                || (data.getHost() != null && data.getHost().contains(resources.getString(R.string.host_name)));
    }

    @Override
    public Screen getScreen() {
        return Screen.UNKNOWN;
    }

    @Override
    protected void setActivityContentView() {
        setContentView(R.layout.resolve);
    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        intentResolver.handleIntent(getIntent(), this);
        finish();
    }

    @Override
    protected boolean receiveConfigurationUpdates() {
        return false;
    }

}
