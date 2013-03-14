package com.soundcloud.android.dao;

import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;

public class PlaylistDAOTest extends BaseDAOTest<PlaylistDAO> {

    public PlaylistDAOTest() {
        super(new PlaylistDAO(Robolectric.application.getContentResolver()));
    }

    @Test
    public void test() throws Exception {
        // TODO: test previously untested methods!
    }
}
