package com.soundcloud.android;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.payments.UpgradeActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
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
import android.net.Uri;

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
    public void openUpgrade() {
        navigator.openUpgrade(activityContext);
        expectStartedActivity(UpgradeActivity.class);
    }

    @Test
    public void opensPlaylist() {
        Urn playlist = Urn.forPlaylist(123L);
        SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.forPlaylist(1234L));

        navigator.openPlaylist(activityContext, playlist, Screen.SEARCH_PLAYLISTS, searchQuerySourceInfo);

        Intent intent = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getExtras().get(PlaylistDetailActivity.EXTRA_URN)).toEqual(playlist);
        expect(intent.getExtras().get(PlaylistDetailActivity.EXTRA_QUERY_SOURCE_INFO)).toEqual(searchQuerySourceInfo);
        expect(Screen.fromIntent(intent)).toEqual(Screen.SEARCH_PLAYLISTS);
    }

    @Test
    public void opensPlaylistWithoutSearchQuerySourceInfo() {
        Urn playlist = Urn.forPlaylist(123L);

        navigator.openPlaylist(activityContext, playlist, Screen.SEARCH_PLAYLISTS);

        Intent intent = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getExtras().get(PlaylistDetailActivity.EXTRA_URN)).toEqual(playlist);
        expect(Screen.fromIntent(intent)).toEqual(Screen.SEARCH_PLAYLISTS);
    }

    @Test
    public void opensMyProfileActivity() {
        navigator.openMyProfile(activityContext, USER_URN);

        Intent intent = expectStartedActivity(MeActivity.class);
        expect(intent.getExtras().get(LegacyProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
    }

    @Test
    public void opensProfileActivity() {
        navigator.openProfile(activityContext, USER_URN);

        Intent intent = expectStartedActivity(LegacyProfileActivity.class);
        expect(intent.getExtras().get(LegacyProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
    }

    @Test
    public void opensProfileActivityWithScreen() {
        navigator.openProfile(activityContext, USER_URN, Screen.DEEPLINK);

        Intent intent = expectStartedActivity(LegacyProfileActivity.class);
        expect(intent.getExtras().get(LegacyProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
        expect(Screen.fromIntent(intent)).toEqual(Screen.DEEPLINK);
    }

    @Test
    public void opensProfileActivityWithSearchSourceInfo() {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L));

        navigator.openProfile(activityContext, USER_URN, Screen.DEEPLINK, searchSourceInfo);

        Intent intent = expectStartedActivity(LegacyProfileActivity.class);
        expect(intent.getExtras().get(LegacyProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
        expect(intent.getExtras().get(LegacyProfileActivity.EXTRA_QUERY_SOURCE_INFO)).not.toBeNull();
        expect(Screen.fromIntent(intent)).toEqual(Screen.DEEPLINK);
    }

    @Test
    public void createsPendingIntentToProfileFromNotification() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = navigator.openProfileFromNotification(appContext, USER_URN);

        pendingIntent.send();

        Intent intent = expectStartedActivity(LegacyProfileActivity.class);
        expect(intent).toHaveFlag(Intent.FLAG_ACTIVITY_NEW_TASK);
        expect(intent.getExtras().get(LegacyProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
        expect(Screen.fromIntent(intent)).toEqual(Screen.NOTIFICATION);
    }

    @Test
    public void createsPendingIntentToProfileFromWidget() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = navigator.openProfileFromWidget(appContext, USER_URN, 0);

        pendingIntent.send();

        Intent intent = expectStartedActivity(LegacyProfileActivity.class);
        expect(intent.getExtras().get(LegacyProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
        expect(Screen.fromIntent(intent)).toEqual(Screen.WIDGET);
    }

    @Test
    public void openRecord() {
        Recording recording = new Recording();
        navigator.openRecord(activityContext, recording);

        Intent intent = expectStartedActivity(RecordActivity.class);
        expect(intent.getExtras().get(Recording.EXTRA)).toEqual(recording);
        expect(intent).toHaveFlag(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }


    @Test
    public void opensOnboarding() {
        navigator.openOnboarding(activityContext, USER_URN, Screen.DEEPLINK);

        Intent intent = expectStartedActivity(OnboardActivity.class);
        expect(intent.getExtras().get(OnboardActivity.EXTRA_URN)).toEqual(USER_URN);
        expect(intent).toHaveFlag(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        expect(Screen.fromIntent(intent)).toEqual(Screen.DEEPLINK);
    }

    @Test
    public void opensStream() {
        navigator.openStream(activityContext, Screen.DEEPLINK);

        Intent intent = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.STREAM);
        expect(Screen.fromIntent(intent)).toEqual(Screen.DEEPLINK);
    }

    @Test
    public void opensLauncher() {
        navigator.openLauncher(activityContext);

        expectStartedActivity(LauncherActivity.class);
    }

    @Test
    public void opensStreamWithExpandedPlayer() {
        navigator.openStreamWithExpandedPlayer(activityContext, Screen.DEEPLINK);

        Intent intent = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.STREAM);
        expect(intent.getExtras().get(SlidingPlayerController.EXTRA_EXPAND_PLAYER)).toBe(true);
        expect(Screen.fromIntent(intent)).toEqual(Screen.DEEPLINK);
    }

    @Test
    public void opensWebView() {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex");
        navigator.openWebView(activityContext, uri);

        Intent intent = expectStartedActivity(WebViewActivity.class);
        expect(intent.getData()).toEqual(uri);
    }

    private Intent expectStartedActivity(Class expected) {
        Intent intent = Robolectric.shadowOf(activityContext).getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getComponent().getClassName()).toEqual(expected.getCanonicalName());
        return intent;
    }
}
