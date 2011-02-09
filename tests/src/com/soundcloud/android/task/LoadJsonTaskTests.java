package com.soundcloud.android.task;


import com.soundcloud.android.api.ApiTest;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class LoadJsonTaskTests extends ApiTest {

    @Test
    public void testList() throws Exception {
        fakeApi("/foo", "[{\"bar\": \"baz\"}]");
        LoadJsonTask<Foo> task = new LoadJsonTask<Foo>(api) {
            @Override
            protected List<Foo> doInBackground(String... strings) {
                return null;
            }
        };

        List<Foo> l = task.list("/foo", Foo.class);
        assertEquals(1, l.size());
        assertEquals("baz", l.get(0).bar);
    }

    public static class Foo {
        public String bar;
    }
}
