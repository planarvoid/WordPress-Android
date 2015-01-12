package com.soundcloud.android.sync.commands;

import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ApiStreamItemFixtures;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class ReplaceSoundStreamCommandTest extends StoreSoundStreamCommandTest {

    @Override
    protected StoreCommand<Iterable<ApiStreamItem>> getStorage() {
        return new ReplaceSoundStreamCommand(propeller());
    }

    @Test
    public void shouldReplaceExistingStreamItem() throws Exception {
        testFixtures().insertStreamTrackPost(ApiStreamItemFixtures.trackPost());
        expectStreamItemCountToBe(1);

        final ApiStreamItem playlistRepost = ApiStreamItemFixtures.playlistRepost();
        getStorage().with(Arrays.asList(playlistRepost)).call();
        expectPlaylistRepostItemInserted(playlistRepost);
        expectStreamItemCountToBe(1);
    }
}