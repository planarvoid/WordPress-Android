package com.soundcloud.android.tests.activity;

import com.soundcloud.android.Actions;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.associations.WhoToFollowActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.upload.UploadActivity;
import com.soundcloud.android.creators.upload.UploadMonitorActivity;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.search.SearchActivity;
import com.soundcloud.android.settings.AccountSettingsActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;

/**
 * Makes sure all intent filters are set up correctly, and the corresponding activities
 * get called.
 */
public class ActivityRoutingTest extends InstrumentationTestCase {

    public void testActivity() throws Exception {
        assertActivityStarted(ActivitiesActivity.class, Actions.ACTIVITY);
    }

    public void testYou() {
        assertActivityStarted(MeActivity.class, Actions.YOU);
        assertActivityStarted(MeActivity.class, Actions.YOUR_INFO);
        assertActivityStarted(MeActivity.class, Actions.YOUR_SOUNDS);
        assertActivityStarted(MeActivity.class, Actions.YOUR_LIKES);
        assertActivityStarted(MeActivity.class, Actions.YOUR_FOLLOWERS);
        assertActivityStarted(MeActivity.class, Actions.YOUR_FOLLOWINGS);
    }

    public void testRecord() {
        assertActivityStarted(RecordActivity.class, Actions.RECORD);
        assertActivityStarted(RecordActivity.class, Actions.RECORD_START);
        assertActivityStarted(RecordActivity.class, Actions.RECORD_STOP);
    }

    public void testStream() throws Exception {
        assertActivityStarted(MainActivity.class, Actions.STREAM);
    }

    public void testShare() throws Exception {
        assertActivityStarted(UploadActivity.class, Actions.SHARE);
    }

    public void testUserBrowser() {
        assertActivityStarted(ProfileActivity.class, Actions.USER_BROWSER);
        assertActivityStarted(ProfileActivity.class, Intent.ACTION_VIEW, Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/users/1235"));
    }

    public void testWhoToFollow() {
        assertActivityStarted(WhoToFollowActivity.class, Actions.WHO_TO_FOLLOW);
    }

    public void testSearch() {
        assertActivityStarted(SearchActivity.class, Actions.SEARCH);
        assertActivityStarted(SearchActivity.class, Intent.ACTION_VIEW, Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/search/1234"));
    }

    public void testResolve() {
        assertActivityStarted(ResolveActivity.class, Intent.ACTION_VIEW, Uri.parse("soundcloud:users:1234"));
        assertActivityStarted(ResolveActivity.class, Intent.ACTION_VIEW, Uri.parse("soundcloud:tracks:1234"));
        assertActivityStarted(ResolveActivity.class, Intent.ACTION_VIEW, Uri.parse("soundcloud:sounds:1234"));
        assertActivityStarted(ResolveActivity.class, Intent.ACTION_VIEW, Uri.parse("soundcloud://users:1234"));
        assertActivityStarted(ResolveActivity.class, Intent.ACTION_VIEW, Uri.parse("soundcloud://tracks:1234"));
        assertActivityStarted(ResolveActivity.class, Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234"));
    }

    public void testAccountSettings() {
        assertActivityStarted(AccountSettingsActivity.class, Actions.ACCOUNT_PREF);
    }

    public void testUploadMonitor() {
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
