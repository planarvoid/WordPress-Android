package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayerUICommandTest {

    @Test
    public void createEventForExpandPlayer() {
        PlayerUICommand event = PlayerUICommand.expandPlayer();
        expect(event.isExpand()).toBeTrue();
    }

    @Test
    public void createsEventForClosePlayer() {
        PlayerUICommand event = PlayerUICommand.collapsePlayer();
        expect(event.isCollapse()).toBeTrue();
    }

    @Test
    public void createsEventForShowPlayer() {
        PlayerUICommand event = PlayerUICommand.showPlayer();
        expect(event.isShow()).toBeTrue();
    }

}