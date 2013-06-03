package com.soundcloud.android;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.tobedevoured.modelcitizen.ModelFactory;
import com.tobedevoured.modelcitizen.RegisterBlueprintException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class BlueprintsTest {

    // this didn't work for me by default for long fields, so using the ConstructorCallback
    // as a workaround
    @Test
    public void shouldGenerateIdFields() throws RegisterBlueprintException, CreateModelException {
        ModelFactory modelFactory = TestHelper.getModelFactory();

        Track track = modelFactory.createModel(Track.class);

        expect(track.getId()).toBe(1L);
        expect(track.getUser().getId()).toBe(1L);
    }

}
