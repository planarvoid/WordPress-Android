package com.soundcloud.android.activity;

import com.soundcloud.android.Actions;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.activity.create.ScUpload;
import com.soundcloud.android.activity.create.UploadMonitor;
import com.soundcloud.android.activity.landing.FriendFinder;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.activity.landing.News;
import com.soundcloud.android.activity.landing.ScSearch;
import com.soundcloud.android.activity.landing.SuggestedUsers;
import com.soundcloud.android.activity.landing.You;
import com.soundcloud.android.activity.settings.AccountSettings;

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
        assertActivityStarted(News.class, Actions.ACTIVITY);
    }

    public void testYou() throws Exception {
        assertActivityStarted(You.class, Actions.YOU);
        assertActivityStarted(You.class, Actions.YOUR_SOUNDS);
        assertActivityStarted(You.class, Actions.YOUR_LIKES);
        assertActivityStarted(You.class, Actions.YOUR_FOLLOWERS);
        assertActivityStarted(You.class, Actions.YOUR_FOLLOWINGS);
    }

    public void testRecord() throws Exception {
        assertActivityStarted(ScCreate.class, Actions.RECORD);
        assertActivityStarted(ScCreate.class, Actions.RECORD_START);
        assertActivityStarted(ScCreate.class, Actions.RECORD_STOP);
    }

    public void testStream() throws Exception {
        assertActivityStarted(Home.class, Actions.STREAM);
    }

    public void testPlayer() throws Exception {
        assertActivityStarted(ScPlayer.class, Actions.PLAYER);
        assertActivityStarted(ScPlayer.class, Intent.ACTION_VIEW, Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/tracks/1235"));
    }

    public void testShare() throws Exception {
        assertActivityStarted(ScUpload.class, Actions.SHARE);
    }

    public void testUserBrowser() throws Exception {
        assertActivityStarted(UserBrowser.class, Actions.USER_BROWSER);
        assertActivityStarted(UserBrowser.class, Intent.ACTION_VIEW, Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/users/1235"));
    }

    public void testFriendFinder() throws Exception {
        assertActivityStarted(FriendFinder.class, Actions.FRIEND_FINDER);
    }

    public void testSuggestedUsers() throws Exception {
        assertActivityStarted(SuggestedUsers.class, Actions.WHO_TO_FOLLOW);
    }

    public void testSearch() throws Exception {
        assertActivityStarted(ScSearch.class, Actions.SEARCH);
        assertActivityStarted(ScSearch.class, Intent.ACTION_VIEW, Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/search/1234"));
    }

    public void testResolve() throws Exception {
        assertActivityStarted(Resolve.class, Intent.ACTION_VIEW, Uri.parse("soundcloud:users:1234"));
        assertActivityStarted(Resolve.class, Intent.ACTION_VIEW, Uri.parse("soundcloud:tracks:1234"));
        assertActivityStarted(Resolve.class, Intent.ACTION_VIEW, Uri.parse("soundcloud:sounds:1234"));
    }

    public void testAccountSettings() throws Exception {
        assertActivityStarted(AccountSettings.class, Actions.ACCOUNT_PREF);
    }

    public void testUploadMonitor() throws Exception {
        assertActivityStarted(UploadMonitor.class, Actions.UPLOAD_MONITOR, Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/recordings/1234"));
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
