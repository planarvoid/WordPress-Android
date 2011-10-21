package com.soundcloud.android.streaming;


import android.app.Activity;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;



@RunWith(DefaultTestRunner.class)
public class StreamItemTest {
     @Test
    public void shouldGetHashKey() throws Exception {
        StreamItem item = new StreamItem(new Activity(), "http://asdf.com");
        assertThat(item.getURLHash(), equalTo("b0ecbe2bc0fd8e426395c81ee96f81cf"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireURL() throws Exception {
        new StreamItem(DefaultTestRunner.application, null);
    }
}
