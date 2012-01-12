package com.soundcloud.android.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.Date;


public class ActivityTest {
    @Test
    public void testEquals() throws Exception {
        Activity a1 = new Activity();
        Activity a2 = new Activity();

        a1.origin = new Track() { { id = 10L; } };
        a2.origin = new Track() { { id = 10L; } };

        assertThat(a1, equalTo(a2));

        Date d = new Date();
        a1.created_at = d;
        assertThat(a1, not(equalTo(a2)));
        a2.created_at = d;

        assertThat(a1, equalTo(a2));

        a1.tags = "foo";
        assertThat(a1, not(equalTo(a2)));
        a2.tags = a1.tags;

        assertThat(a1, equalTo(a2));

        a1.type = "track";

        assertThat(a1, not(equalTo(a2)));
        a2.type = a1.type;
        assertThat(a1, equalTo(a2));
    }
}
