package com.soundcloud.android.activity;

import com.soundcloud.android.Actions;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.main.ResolveActivity;
import com.soundcloud.android.preferences.AccountSettingsActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.associations.FriendFinderActivity;
import com.soundcloud.android.playback.PlayerActivity;
import com.soundcloud.android.search.SearchActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.upload.UploadActivity;
import com.soundcloud.android.creators.upload.UploadMonitorActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;

/**
 * Makes sure all intent filters are set up correctly, and the corresponding activities
 * get called.
 */
public class ActivityRoutingTest extends InstrumentationTestCase {

    public void ignore_testActivity() throws Exception {
        assertActivityStarted(ActivitiesActivity.class, Actions.ACTIVITY);
    }

    public void ignore_testYou() throws Exception {
        assertActivityStarted(MeActivity.class, Actions.YOU);
        assertActivityStarted(MeActivity.class, Actions.YOUR_INFO);
        assertActivityStarted(MeActivity.class, Actions.YOUR_SOUNDS);
        assertActivityStarted(MeActivity.class, Actions.YOUR_LIKES);
        assertActivityStarted(MeActivity.class, Actions.YOUR_FOLLOWERS);
        assertActivityStarted(MeActivity.class, Actions.YOUR_FOLLOWINGS);
    }

    public void ignore_testRecord() throws Exception {
        assertActivityStarted(RecordActivity.class, Actions.RECORD);
        assertActivityStarted(RecordActivity.class, Actions.RECORD_START);
        assertActivityStarted(RecordActivity.class, Actions.RECORD_STOP);
    }

    public void ignore_testStream() throws Exception {
        assertActivityStarted(MainActivity.class, Actions.STREAM);
    }

    public void ignore_testPlayer() throws Exception {
        assertActivityStarted(PlayerActivity.class, Actions.PLAYER);
        assertActivityStarted(PlayerActivity.class, Intent.ACTION_VIEW, Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/tracks/1235"));
    }

    public void ignore_testPlayWithPlaylist() throws Exception {
        assertActivityStarted(PlayerActivity.class, Actions.PLAY,  Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/me/tracks"));
    }

    public void ignore_testPlayEmpty() throws Exception {
        assertActivityStarted(PlayerActivity.class, Actions.PLAY);
    }

    public void ignore_testShare() throws Exception {
        assertActivityStarted(UploadActivity.class, Actions.SHARE);
    }

    public void ignore_testUserBrowser() throws Exception {
        assertActivityStarted(ProfileActivity.class, Actions.USER_BROWSER);
        assertActivityStarted(ProfileActivity.class, Intent.ACTION_VIEW, Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/users/1235"));
    }

    public void ignore_testFriendFinder() throws Exception {
        assertActivityStarted(FriendFinderActivity.class, Actions.FRIEND_FINDER);
    }

    public void ignore_testSuggestedUsers() throws Exception {
        assertActivityStarted(SuggestedUsersActivity.class, Actions.WHO_TO_FOLLOW);
    }

    public void ignore_testSearch() throws Exception {
        assertActivityStarted(SearchActivity.class, Actions.SEARCH);
        assertActivityStarted(SearchActivity.class, Intent.ACTION_VIEW, Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/search/1234"));
    }

    public void ignore_testResolve() throws Exception {
        assertActivityStarted(ResolveActivity.class, Intent.ACTION_VIEW, Uri.parse("soundcloud:users:1234"));
        assertActivityStarted(ResolveActivity.class, Intent.ACTION_VIEW, Uri.parse("soundcloud:tracks:1234"));
        assertActivityStarted(ResolveActivity.class, Intent.ACTION_VIEW, Uri.parse("soundcloud:sounds:1234"));
    }

    public void ignore_testAccountSettings() throws Exception {
        assertActivityStarted(AccountSettingsActivity.class, Actions.ACCOUNT_PREF);
    }

    public void ignore_testUploadMonitor() throws Exception {
        assertActivityStarted(UploadMonitorActivity.class, Actions.UPLOAD_MONITOR, Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/recordings/1234"));
    }

    private void assertActivityStarted(Class<? extends Activity> expectedActivity, String action, Uri... data) {
        Intent intent = new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (data != null && data.length > 0) {
            intent.setData(data[0]);
        }

        Activity activity = getInstrumentation().startActivitySync(intent);
        getInstrumentation().waitForIdleSync();
        activity.finish();
        assertTrue("activity " + activity + " is not instance of " + expectedActivity,
                expectedActivity.isAssignableFrom(activity.getClass()));
    }
}
