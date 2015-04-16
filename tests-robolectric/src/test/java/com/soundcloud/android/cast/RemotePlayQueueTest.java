package com.soundcloud.android.cast;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.common.collect.Lists;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class RemotePlayQueueTest {

    private static final Urn URN = Urn.forTrack(123L);
    private static final Urn URN2 = Urn.forTrack(456L);
    private static final List<Urn> PLAY_QUEUE = Arrays.asList(URN, URN2);

    @Test
    public void returnsCurrentTrackPosition() {
        RemotePlayQueue remotePlayQueue = new RemotePlayQueue(PLAY_QUEUE, URN2);
        expect(remotePlayQueue.getCurrentPosition()).toEqual(1);
    }

}