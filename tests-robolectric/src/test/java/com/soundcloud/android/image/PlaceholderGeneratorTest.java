package com.soundcloud.android.image;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlaceholderGeneratorTest {

    private PlaceholderGenerator generator;

    @Before
    public void setUp() throws Exception {
        generator = new PlaceholderGenerator(Robolectric.application.getResources());
    }

    @Test
    public void shouldPickSameIndexForDifferentInstancesOfSameKey() {
        int index1 = generator.pickCombination("soundcloud:something:47");
        int index2 = generator.pickCombination("soundcloud:something:47");

        expect(index1).toEqual(index2);
    }

    @Test
    public void shouldPickDifferentIndexForDifferentKeys() {
        int index1 = generator.pickCombination("soundcloud:something:47");
        int index2 = generator.pickCombination("soundcloud:something:48");

        expect(index1).not.toEqual(index2);
    }

    /*
     * This test guards against a regression: we can't use Math.abs() with string.hashCode()
     * See: http://findbugs.blogspot.de/2006/09/is-mathabs-broken.html
     */
    @Test
    public void shouldPickWithinArrayBoundsForKeyWithHashCodeOfMinValue() {
        String key = "DESIGNING WORKHOUSES"; // Has a hashCode of Integer.MIN_VALUE

        int index = generator.pickCombination(key);

        expect(index).toBeGreaterThan(-1);
    }

}
