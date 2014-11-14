package com.soundcloud.android.coreutils.text;

import static com.soundcloud.android.coreutils.text.Strings.allInArrayAreBlank;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class StringsTest {


    @Test
    public void shouldReturnTrueIfNullArrayProvided(){
        assertThat(allInArrayAreBlank((String[])null), is(true));
    }

    @Test
    public void shouldReturnTrueIfEmptyArrayProvided(){
        assertThat(allInArrayAreBlank(new String[]{}), is(true));
    }

    @Test
    public void shouldReturnTrueIfAllStringsInArrayAreBlank(){
        assertThat(allInArrayAreBlank(new String[]{"", "  "}), is(true));
    }

    @Test
    public void shouldReturnFalseIfAllStringsInArrayAreNotBlank(){
        assertThat(allInArrayAreBlank(new String[]{"", "  ", "a"}), is(false));
    }

}