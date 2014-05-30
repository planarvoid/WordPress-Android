package com.soundcloud.android.collections.views;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static com.soundcloud.android.Expect.expect;

@RunWith(SoundCloudTestRunner.class)
public class PlayableRowTest {
    private PlayableRow playableRow;

    @Mock
    private ImageOperations imageOperations;
    ;

    @Before
    public void setUp() throws Exception {
        playableRow = new PlayableRow(Robolectric.application, imageOperations);
    }

    @Test
    public void shouldReturnInformationNotAvailableWhenTheContentNotAvailable() throws Exception {
        expect(playableRow.getContentDescription()).toEqual(Robolectric.application.getString(R.string.no_info_available));
    }
}