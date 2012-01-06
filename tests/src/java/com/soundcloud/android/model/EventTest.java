package com.soundcloud.android.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.Date;


public class EventTest {
    @Test
    public void testEquals() throws Exception {
        Event e1 = new Event();
        Event e2 = new Event();

        e1.origin = new Track() { { id = 10L; } };
        e2.origin = new Track() { { id = 10L; } };

        assertThat(e1, equalTo(e2));

        Date d = new Date();
        e1.created_at = d;
        assertThat(e1, not(equalTo(e2)));
        e2.created_at = d;

        assertThat(e1, equalTo(e2));

        e1.tags = "foo";
        assertThat(e1, not(equalTo(e2)));
        e2.tags = e1.tags;

        assertThat(e1, equalTo(e2));

        e1.type = "track";

        assertThat(e1, not(equalTo(e2)));
        e2.type = e1.type;
        assertThat(e1, equalTo(e2));
    }
}
