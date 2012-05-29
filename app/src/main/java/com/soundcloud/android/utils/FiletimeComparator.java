package com.soundcloud.android.utils;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class FiletimeComparator implements Comparator<File> {
    private Map<File, Long> filetimes = new HashMap<File, Long>();
    private boolean oldestFirst;

    public FiletimeComparator(boolean oldestFirst) {
        this.oldestFirst = oldestFirst;
    }

    @Override
    public int compare(File f1, File f2) {
        return oldestFirst ?
                getTimestamp(f1, filetimes).compareTo(getTimestamp(f2, filetimes)) :
                getTimestamp(f2, filetimes).compareTo(getTimestamp(f1, filetimes));
    }

    private static Long getTimestamp(File f, Map<File, Long> map) {
        final Long l = map.get(f);
        if (l == null) {
            // cache file modification time to prevent
            // IllegalArgumentException: Comparison method violates its general contract!
            final long lm = f.lastModified();
            map.put(f, lm);
            return lm;
        } else {
            return l;
        }
    }
}
