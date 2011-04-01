package com.soundcloud.android.task;


import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.BaseApiTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(DefaultTestRunner.class)
public class LoadJsonTaskTests extends BaseApiTest {

    @Test
    public void shouldReturnAList() throws Exception {
        expectGetRequestAndReturn("/foo", "[{\"bar\": \"baz\"}]");
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

    @Test
    public void shouldReturnNullWhenExceptionEncountered() throws Exception {
        expectGetRequestAndThrow("/foo", new IOException());

        LoadJsonTask<Foo> task = new LoadJsonTask<Foo>(api) {
            @Override
            protected List<Foo> doInBackground(String... strings) {
                return null;
            }
        };
        List<Foo> l = task.list("/foo", Foo.class);
        assertNull(l);
    }

    @Test(expected = RuntimeException.class)
    public void shouldReraiseExceptionWhenTold() throws Exception {
        expectGetRequestAndThrow("/foo", new IOException());

        LoadJsonTask<Foo> task = new LoadJsonTask<Foo>(api) {
            @Override
            protected List<Foo> doInBackground(String... strings) {
                return null;
            }
        };
        task.list("/foo", Foo.class, true);
    }

    public static class Foo {
        public String bar;
    }
}
