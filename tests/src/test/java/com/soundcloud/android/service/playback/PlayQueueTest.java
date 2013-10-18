package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueTest {

    @Test
    public void shouldBeParcelable() throws Exception {
        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(1L).exploreTag("explore").originUrl("url/123").recommenderVersion("version1").build();
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L,2L,3L), 0, playSourceInfo);
        playQueue.setAppendState(PlayQueue.AppendState.IDLE);

        Parcel parcel = Parcel.obtain();
        playQueue.writeToParcel(parcel, 0);
        PlayQueue copy = PlayQueue.CREATOR.createFromParcel(parcel);

        expect(copy.getCurrentTrackIds()).toContainExactly(1L,2L,3L);
        expect(copy.getPlayPosition()).toBe(0);
        expect(copy.getAppendState()).toEqual(PlayQueue.AppendState.IDLE);
        expect(copy.getPlaySourceInfo()).toEqual(playSourceInfo);
    }
}
