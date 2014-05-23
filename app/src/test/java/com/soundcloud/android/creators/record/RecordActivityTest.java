package com.soundcloud.android.creators.record;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static junit.framework.Assert.assertNotSame;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

@RunWith(DefaultTestRunner.class)
public class RecordActivityTest {

    RecordActivity activity;

    @Before
    public void setUp() throws Exception {
        activity = new RecordActivity();
    }

    @Test
    public void shouldFindAllSuggestions() throws Exception {
        checkKeys(DefaultTestRunner.application.getResources().getStringArray(R.array.record_suggestion_keys));
    }

    @Test
    public void shouldFindAllPrivateSuggestions() throws Exception {
        checkKeys(DefaultTestRunner.application.getResources().getStringArray(R.array.record_suggestion_keys_private));
    }

    private void checkKeys(String[] keys) throws Exception {
        for (String key : keys) {
            assertNotSame("[string resource needed for record suggestions with keyname " + RecordMessageView.STRING_RESOURCE_PREFIX + key + "]",
                    DefaultTestRunner.application.getResources().getIdentifier(RecordMessageView.STRING_RESOURCE_PREFIX + key, "string", DefaultTestRunner.application.getPackageName()), 0);
        }
    }

    @Test
    public void shouldGoToStreamOnNavigationUp() throws Exception {
        activity.onSupportNavigateUp();
        Intent nextStartedActivity = shadowOf(activity).getNextStartedActivity();
        expect(nextStartedActivity.getAction()).toEqual(Actions.STREAM);
        expect(nextStartedActivity.getFlags()).toEqual(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Test
    public void shouldFinishOnNavigationUp() throws Exception {
        activity.onSupportNavigateUp();
        expect(activity.isFinishing()).toBeTrue();
    }
}
