package com.soundcloud.android.creators.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class CurrentAmplitudeHelperTest {
    private static final float FLOAT_DELTA = .0001f;

    private static final int BAR_COUNT = 2;
    private static final int SPACE_COUNT = 2;

    CreateWaveView.CurrentAmplitudeHelper helper;

    @Before
    public void setUp() throws Exception {
        helper = new CreateWaveView.CurrentAmplitudeHelper(BAR_COUNT, SPACE_COUNT);
    }

    @Test
    public void currentValueIsSingleValue() throws Exception {
        helper.updateAmplitude(.5f);
        expect(helper.currentValue()).toEqual(.5f);
    }

    @Test
    public void currentValueIsFirstValueWithinBarCount() throws Exception {
        helper.updateAmplitude(.5f); // first bar
        helper.updateAmplitude(.3f); // second bar
        expect(helper.currentValue()).toEqual(.5f);
    }

    @Test
    public void shoudlShowSpaceInsideBarCount() throws Exception {
        helper.updateAmplitude(.5f); // first bar
        helper.updateAmplitude(.5f); // second bar
        expect(helper.shouldShowSpace()).toBeFalse();
    }

    @Test
    public void shoudlShowSpaceIsTrueWithinSpaceCount() throws Exception {
        helper.updateAmplitude(.5f); // first bar
        helper.updateAmplitude(.5f); // second bar
        helper.updateAmplitude(.3f); // first space
        expect(helper.shouldShowSpace()).toBeTrue();
    }

    @Test
    public void firstSpaceIsAverageOfSecondBarAndFirstSpace() throws Exception {
        helper.updateAmplitude(.5f); // first bar
        helper.updateAmplitude(.5f); // second bar
        helper.updateAmplitude(.3f); // first space
        expect(helper.currentValue()).toEqual(.4f);// expect an average of second bar and first space
    }

    @Test
    public void secondSpaceIsSameValueAsFirstSpace() throws Exception {
        helper.updateAmplitude(.5f); // first bar
        helper.updateAmplitude(.5f); // second bar
        helper.updateAmplitude(.3f);
        final float firstSpace = helper.currentValue();
        helper.updateAmplitude(.9f);
        expect(helper.currentValue()).toEqual(firstSpace); // expect an average of second bar and first space
    }

    @Test
    public void secondBarGroupUsesFirstSpaceValueIfAverageHasGoneDown() throws Exception {
        helper.updateAmplitude(.5f); // first bar
        helper.updateAmplitude(.5f); // second bar

        helper.updateAmplitude(.3f); // first space
        helper.updateAmplitude(.5f); // second space

        helper.updateAmplitude(.001f); // first bar of second group

        assertFloatsEqual(helper.currentValue(), .4f); // expect an average of second bar and first space
    }

    @Test
    public void secondBarGroupUsesNewAvaerageIfAverageHasGoneUp() throws Exception {
        helper.updateAmplitude(.5f); // first bar
        helper.updateAmplitude(.5f); // second bar

        helper.updateAmplitude(.3f); // first space
        helper.updateAmplitude(.5f); // second space

        helper.updateAmplitude(.9f); // first bar

        assertFloatsEqual(helper.currentValue(), .7f); // expect an average of second space and first bar of second group
    }

    private void assertFloatsEqual(float f1, float f2){
        expect(Math.abs(f1 - f2)).toBeLessThan(FLOAT_DELTA);
    }
}