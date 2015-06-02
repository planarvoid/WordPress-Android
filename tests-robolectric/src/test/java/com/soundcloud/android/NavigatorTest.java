package com.soundcloud.android;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.LegacyProfileActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class NavigatorTest {

    private static final Urn USER_URN = Urn.forUser(123L);

    @Mock private FeatureFlags flags;

    private Navigator navigator;

    private Context appContext;
    private Activity activityContext;

    @Before
    public void setUp() throws Exception {
        navigator = new Navigator(flags);
        appContext = Robolectric.application;
        activityContext = new Activity();
        when(flags.isEnabled(Flag.NEW_PROFILE)).thenReturn(false);
    }

    @Test
    public void opensMyProfileActivity() {
        navigator.openMyProfile(activityContext, USER_URN);

        Intent startedActivity = Robolectric.shadowOf(activityContext).getNextStartedActivity();
        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getComponent().getClassName()).toEqual(MeActivity.class.getCanonicalName());
        expect(startedActivity.getExtras().get(LegacyProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
    }

    @Test
    public void opensProfileActivity() {
        navigator.openProfile(activityContext, USER_URN);

        Intent startedActivity = Robolectric.shadowOf(activityContext).getNextStartedActivity();
        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getComponent().getClassName()).toEqual(LegacyProfileActivity.class.getCanonicalName());
        expect(startedActivity.getExtras().get(LegacyProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
    }

    @Test
    public void opensProfileActivityWithSearchSourceInfo() {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L));

        navigator.openProfile(activityContext, USER_URN, searchSourceInfo);

        Intent startedActivity = Robolectric.shadowOf(activityContext).getNextStartedActivity();
        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getComponent().getClassName()).toEqual(LegacyProfileActivity.class.getCanonicalName());
        expect(startedActivity.getExtras().get(LegacyProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
        expect(startedActivity.getExtras().get(LegacyProfileActivity.EXTRA_QUERY_SOURCE_INFO)).not.toBeNull();
    }

    @Test
    public void createsPendingIntentToProfileFromNotification() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = navigator.openProfileFromNotification(appContext, USER_URN);

        pendingIntent.send();

        Intent startedActivity = Robolectric.shadowOf(activityContext).getNextStartedActivity();
        expect(startedActivity).not.toBeNull();
        expect(startedActivity).toHaveFlag(Intent.FLAG_ACTIVITY_NEW_TASK);
        expect(startedActivity.getComponent().getClassName()).toEqual(LegacyProfileActivity.class.getCanonicalName());
        expect(startedActivity.getExtras().get(LegacyProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
    }

    @Test
    public void createsPendingIntentToProfileFromWidget() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = navigator.openProfileFromWidget(appContext, USER_URN, 0);

        pendingIntent.send();

        Intent startedActivity = Robolectric.shadowOf(activityContext).getNextStartedActivity();
        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getComponent().getClassName()).toEqual(LegacyProfileActivity.class.getCanonicalName());
        expect(startedActivity.getExtras().get(LegacyProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
    }

}