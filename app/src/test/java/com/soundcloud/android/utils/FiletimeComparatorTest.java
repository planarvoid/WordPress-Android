package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class FiletimeComparatorTest {

    @Test
    public void testLastModfifiedFileComparatorOldestFirst() throws Exception {
        File f1 = File.createTempFile("test_f1", null);
        File f2 = File.createTempFile("test_f2", null);

        f1.setLastModified(System.currentTimeMillis());
        f2.setLastModified(f1.lastModified() + 3000);

        List<File> files = Arrays.asList(f2, f1);

        Collections.sort(files, new FiletimeComparator(true));

        assertThat(files.get(0)).isEqualTo(f1);
        assertThat(files.get(1)).isEqualTo(f2);
    }

    @Test
    public void testLastModifiedFileComparatorNewestFirst() throws Exception {
        File f1 = File.createTempFile("test_f1", null);
        File f2 = File.createTempFile("test_f2", null);

        f1.setLastModified(System.currentTimeMillis());
        f2.setLastModified(f1.lastModified() + 3000);

        List<File> files = Arrays.asList(f2, f1);

        Collections.sort(files, new FiletimeComparator(false));

        assertThat(files.get(0)).isEqualTo(f2);
        assertThat(files.get(1)).isEqualTo(f1);
    }
}
