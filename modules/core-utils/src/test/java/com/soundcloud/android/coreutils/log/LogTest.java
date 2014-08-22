package com.soundcloud.android.coreutils.log;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.slf4j.Logger;

public class LogTest {

    @Test
    public void shouldRetrieveLoggerWithExpectedName() {
        Logger logger = Log.getLogger();
        assertThat(logger.getName(), is("sclog.LogTest"));
    }

}