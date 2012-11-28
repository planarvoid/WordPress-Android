package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class ContentStatsTest {
    @Test
    public void shouldSetLastSeen() throws Exception {
        ContentStats.setLastSeen(Robolectric.application, Content.ME_SOUND_STREAM, 2000);
        expect(ContentStats.getLastSeen(Robolectric.application, Content.ME_SOUND_STREAM)).toEqual(2000l);

        ContentStats.setLastNotified(Robolectric.application, Content.ME_SOUND_STREAM, 3000);
        expect(ContentStats.getLastNotified(Robolectric.application, Content.ME_SOUND_STREAM)).toEqual(3000l);

        ContentStats.setLastNotifiedItem(Robolectric.application, Content.ME_SOUND_STREAM, 4000);
        expect(ContentStats.getLastNotifiedItem(Robolectric.application, Content.ME_SOUND_STREAM)).toEqual(4000l);
    }

    @Test
    public void shouldInit() throws Exception {
        ContentStats.init(Robolectric.application);

        expect(ContentStats.count(Content.ME_SOUND_STREAM)).toEqual(0);
        expect(ContentStats.count(Content.ME_ACTIVITIES)).toEqual(0);
    }

}
