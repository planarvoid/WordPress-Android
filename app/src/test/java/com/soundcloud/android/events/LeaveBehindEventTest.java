package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class LeaveBehindEventTest {

    @Test
    public void createsEventFromPlayerExpanded() {
        LeaveBehindEvent event = LeaveBehindEvent.shown();
        expect(event.getKind()).toEqual(0);
    }

    @Test
    public void createsEventFromPlayerCollapsed() {
        LeaveBehindEvent event = LeaveBehindEvent.hidden();
        expect(event.getKind()).toEqual(1);
    }

}