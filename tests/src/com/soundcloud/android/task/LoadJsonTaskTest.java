package com.soundcloud.android.task;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.RoboApiBaseTests;
import com.soundcloud.api.Request;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class LoadJsonTaskTest extends RoboApiBaseTests {
    @Test
    public void shouldReturnAList() throws Exception {
        expectGetRequestAndReturn("/foo", 200, "[{\"bar\": \"baz\"}]");
        LoadJsonTask<Foo> task = new LoadJsonTask<Foo>(mockedApi) {
            @Override
            protected List<Foo> doInBackground(Request... r) {
                return null;
            }
        };

        List<Foo> l = task.list(Request.to("/foo"), Foo.class);
        assertEquals(1, l.size());
        assertEquals("baz", l.get(0).bar);
    }

    @Test
    public void shouldReturnNullWhenExceptionEncountered() throws Exception {
        expectGetRequestAndThrow("/foo", new IOException());

        LoadJsonTask<Foo> task = new LoadJsonTask<Foo>(mockedApi) {
            @Override
            protected List<Foo> doInBackground(Request... strings) {
                return null;
            }
        };
        List<Foo> l = task.list(Request.to("/foo"), Foo.class);
        assertNull(l);
    }

    @Test(expected = RuntimeException.class)
    public void shouldReraiseExceptionWhenTold() throws Exception {
        expectGetRequestAndThrow("/foo", new IOException());

        LoadJsonTask<Foo> task = new LoadJsonTask<Foo>(mockedApi) {
            @Override
            protected List<Foo> doInBackground(Request... r) {
                return null;
            }
        };
        task.list(Request.to("/foo"), Foo.class, true);
    }

    public static class Foo {
        public String bar;
    }
}
