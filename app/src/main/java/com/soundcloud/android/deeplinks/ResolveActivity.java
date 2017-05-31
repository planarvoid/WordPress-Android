package com.soundcloud.android.deeplinks;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.navigation.Navigator;

import android.content.res.Resources;
import android.net.Uri;

import javax.inject.Inject;

public class ResolveActivity extends RootActivity {

    @Inject ReferrerResolver referrerResolver;
    @Inject Navigator navigator;

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
        setContentView(R.layout.empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Uri uri = getIntent().getData();
        final String referrer = referrerResolver.getReferrerFromIntent(getIntent(), getResources());
        navigator.navigateTo(NavigationTarget.forExternalDeeplink(this, uri.toString(), referrer));
    }

    @Override
    protected boolean receiveConfigurationUpdates() {
        return false;
    }
}
