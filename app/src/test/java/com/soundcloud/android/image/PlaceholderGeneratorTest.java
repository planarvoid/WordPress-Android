package com.soundcloud.android.image;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

public class PlaceholderGeneratorTest extends AndroidUnitTest {

    private PlaceholderGenerator generator;

    @Before
    public void setUp() throws Exception {
        generator = new PlaceholderGenerator(resources());
    }

    @Test
    public void shouldPickSameIndexForDifferentInstancesOfSameKey() {
        int index1 = generator.pickCombination("soundcloud:something:47");
        int index2 = generator.pickCombination("soundcloud:something:47");

        assertThat(index1).isEqualTo(index2);
    }

    @Test
    public void shouldPickDifferentIndexForDifferentKeys() {
        int index1 = generator.pickCombination("soundcloud:something:47");
        int index2 = generator.pickCombination("soundcloud:something:48");

        assertThat(index1).isNotEqualTo(index2);
    }

    /*
     * This test guards against a regression: we can't use Math.abs() with string.hashCode()
     * See: http://findbugs.blogspot.de/2006/09/is-mathabs-broken.html
     */
    @Test
    public void shouldPickWithinArrayBoundsForKeyWithHashCodeOfMinValue() {
        String key = "DESIGNING WORKHOUSES"; // Has a hashCode of Integer.MIN_VALUE

        int index = generator.pickCombination(key);

        assertThat(index).isGreaterThan(-1);
    }

}
