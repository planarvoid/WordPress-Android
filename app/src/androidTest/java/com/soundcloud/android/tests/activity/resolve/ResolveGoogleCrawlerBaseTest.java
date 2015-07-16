package com.soundcloud.android.tests.activity.resolve;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;
import android.net.Uri;

public abstract class ResolveGoogleCrawlerBaseTest extends ActivityTest<ResolveActivity> {
    public ResolveGoogleCrawlerBaseTest() {
        super(ResolveActivity.class);
    }

    protected abstract Uri getUri();

    @Override
    protected void setUp() throws Exception {
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(getUri());
        intent.putExtra("android.intent.extra.REFERRER", Uri.parse("android-app://com.google.appcrawler"));
        setActivityIntent(intent);
        super.setUp();
    }
}
