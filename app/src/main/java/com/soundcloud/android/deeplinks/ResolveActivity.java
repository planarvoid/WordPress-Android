package com.soundcloud.android.deeplinks;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.TrackedActivity;
import com.soundcloud.android.model.Urn;

import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

public class ResolveActivity extends TrackedActivity {

    @Inject IntentResolver intentResolver;

    public ResolveActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    public static boolean accept(Uri data, Resources resources) {
        return Urn.SOUNDCLOUD_SCHEME.equalsIgnoreCase(data.getScheme())
                || (data.getHost() != null && data.getHost().contains(resources.getString(R.string.host_name)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resolve);
    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        intentResolver.handleIntent(getIntent(), this);
        finish();
    }
}
