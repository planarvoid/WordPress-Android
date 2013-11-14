package com.soundcloud.android.creators.record;

import static junit.framework.Assert.assertNotSame;

import com.soundcloud.android.R;
import com.soundcloud.android.creators.record.RecordMessageView;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class RecordActivityTest {

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
}
