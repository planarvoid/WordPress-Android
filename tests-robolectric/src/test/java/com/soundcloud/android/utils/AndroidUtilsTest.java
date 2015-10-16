package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;

@RunWith(DefaultTestRunner.class)
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

        expect(Arrays.equals(actual, expected)).toBeTrue();
    }

}
