package com.soundcloud.android.task;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class LoadJsonTaskTest {
    @Test
    public void shouldReturnAList() throws Exception {
        Robolectric.addPendingHttpResponse(200,"[{\"bar\": \"baz\"}]" );
        LoadJsonTask<Void, Foo> task = new LoadJsonTask<Void, Foo>(DefaultTestRunner.application) {
            @Override
            protected List<Foo> doInBackground(Void... r) {
                return null;
            }
        };

        List<Foo> l = task.list(Request.to("/foo"), Foo.class);
        assertEquals(1, l.size());
        assertEquals("baz", l.get(0).bar);
    }

    @Test
    public void shouldReturnNullWhenExceptionEncountered() throws Exception {
        TestHelper.addPendingIOException("/foo");

        LoadJsonTask<Void, Foo> task = new LoadJsonTask<Void, Foo>(DefaultTestRunner.application) {
            @Override
            protected List<Foo> doInBackground(Void... strings) {
                return null;
            }
        };
        List<Foo> l = task.list(Request.to("/foo"), Foo.class);
        assertNull(l);
    }

    @Test(expected = RuntimeException.class)
    public void shouldReraiseExceptionWhenTold() throws Exception {
        TestHelper.addPendingIOException("/foo");

        LoadJsonTask<Void, Foo> task = new LoadJsonTask<Void,Foo>(DefaultTestRunner.application) {
            @Override
            protected List<Foo> doInBackground(Void... r) {
                return null;
            }
        };
        task.list(Request.to("/foo"), Foo.class, true);
    }

    public static class Foo {
        public String bar;
    }
}
