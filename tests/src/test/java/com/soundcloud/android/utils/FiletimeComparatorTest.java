package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

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

        expect(files.get(0)).toEqual(f1);
        expect(files.get(1)).toEqual(f2);
    }


    @Test
    public void testLastModfifiedFileComparatorNewestFirst() throws Exception {
        File f1 = File.createTempFile("test_f1", null);
        File f2 = File.createTempFile("test_f2", null);

        f1.setLastModified(System.currentTimeMillis());
        f2.setLastModified(f1.lastModified() + 3000);

        List<File> files = Arrays.asList(f2, f1);

        Collections.sort(files, new FiletimeComparator(false));

        expect(files.get(0)).toEqual(f2);
        expect(files.get(1)).toEqual(f1);
    }
}
