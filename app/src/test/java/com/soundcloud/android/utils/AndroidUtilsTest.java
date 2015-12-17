package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

public class AndroidUtilsTest {

    @Test
    public void shouldReturnKeysSortedByValue() throws Exception {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("d",1);
        map.put("b",3);
        map.put("a",4);
        map.put("c",2);

        final String[] actual = AndroidUtils.returnKeysSortedByValue(map);
        final String[] expected = {"a", "b", "c", "d"};

        assertThat(Arrays.equals(actual, expected)).isTrue();
    }

}
