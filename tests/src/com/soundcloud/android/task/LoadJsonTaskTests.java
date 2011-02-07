package com.soundcloud.android.task;


import com.soundcloud.android.CloudAPI;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LoadJsonTaskTests {
    CloudAPI api;

    @Before
    public void setUp() throws Exception {
        api = mock(CloudAPI.class);
    }

    @Test
    public void testList() throws Exception {
        when(api.executeRequest("/foo")).thenReturn(new ByteArrayInputStream("[{\"bar\": \"baz\"}]".getBytes()));

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
