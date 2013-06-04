package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ScModelTest {

    private ScModel model = new ScModel("soundcloud:users:1");

    @Test
    public void shouldUseIdFromIdFieldIfSet() {
        model.setId(2L);
        expect(model.getId()).toBe(2L);
    }

    @Test
    public void shouldParseIdFromUrnIfIdFieldNotSet() {
        model.setId(ScModel.NOT_SET);
        expect(model.getId()).toBe(1L);
    }

    @Test
    public void shouldReturnIdNotSetIfNoUrnSet() {
        expect((int) new SuggestedUser().getId()).toEqual(ScModel.NOT_SET);
    }
}
