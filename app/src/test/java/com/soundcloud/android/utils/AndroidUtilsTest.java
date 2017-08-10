package com.soundcloud.android.utils;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.os.Bundle;

import java.util.Arrays;
import java.util.HashMap;

public class AndroidUtilsTest extends AndroidUnitTest {

    @Test
    public void shouldReturnKeysSortedByValue() throws Exception {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("d", 1);
        map.put("b", 3);
        map.put("a", 4);
        map.put("c", 2);

        final String[] actual = AndroidUtils.returnKeysSortedByValue(map);
        final String[] expected = {"a", "b", "c", "d"};

        assertThat(Arrays.equals(actual, expected)).isTrue();
    }

    @Test
    public void callingDumpToStringWithNullBundleShouldReturnEmptyString() {

        String string = AndroidUtils.dumpBundleToString(null);

        assertThat(string).isEmpty();
    }

    @Test
    public void callingDumpToStringWithBundleShouldReturnAStringRepresentation() {

        Bundle b = new Bundle();
        b.putString("key", "value");
        String string = AndroidUtils.dumpBundleToString(b);

        assertThat(string).isEqualToIgnoringCase("key = value");
    }
}
