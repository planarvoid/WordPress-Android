package com.soundcloud.android.creators.record;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static junit.framework.Assert.assertNotSame;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.content.res.Resources;

@RunWith(SoundCloudTestRunner.class)
public class RecordActivityTest {

    private RecordActivity activity;
    private Resources resources;

    @Before
    public void setUp() throws Exception {
        activity = new RecordActivity();
        resources = Robolectric.application.getResources();
    }

    @Test
    public void shouldFindAllSuggestions() throws Exception {
        checkKeys(resources.getStringArray(R.array.record_suggestion_keys));
    }

    @Test
    public void shouldFindAllPrivateSuggestions() throws Exception {
        checkKeys(resources.getStringArray(R.array.record_suggestion_keys_private));
    }

    private void checkKeys(String[] keys) throws Exception {
        for (String key : keys) {
            assertNotSame("[string resource needed for record suggestions with keyname " + RecordMessageView.STRING_RESOURCE_PREFIX + key + "]",
                    resources.getIdentifier(RecordMessageView.STRING_RESOURCE_PREFIX + key, "string", Robolectric.application.getPackageName()), 0);
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
