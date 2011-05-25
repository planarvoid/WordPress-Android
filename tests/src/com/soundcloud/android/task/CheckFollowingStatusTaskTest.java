package com.soundcloud.android.task;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.RoboApiBaseTests;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;


@RunWith(DefaultTestRunner.class)
public class CheckFollowingStatusTaskTest extends RoboApiBaseTests {
    @Test
    public void shouldReturnFalseIfNotFollowing() throws Exception {
        expectGetRequestAndReturn(null, 404, null);
        CheckFollowingStatusTask task = new CheckFollowingStatusTask(mockedApi);
        assertThat(task.doInBackground(1000), is(false));
    }

    @Test
    public void shouldReturnTrueIfFollowing() throws Exception {
        expectGetRequestAndReturn(null, 303, null);
        CheckFollowingStatusTask task = new CheckFollowingStatusTask(mockedApi);
        assertThat(task.doInBackground(1000), is(true));
    }

    @Test
    public void shouldReturnNullIfUndecided() throws Exception {
        expectGetRequestAndReturn(null, 666, null);
        CheckFollowingStatusTask task = new CheckFollowingStatusTask(mockedApi);
        assertThat(task.doInBackground(1000), nullValue());
    }

    @Test
    public void shouldReturnNullIfExceptionRaised() throws Exception {
        expectGetRequestAndThrow(null, new IOException());
        CheckFollowingStatusTask task = new CheckFollowingStatusTask(mockedApi);
        assertThat(task.doInBackground(1000), nullValue());
    }
}
