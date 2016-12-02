package com.soundcloud.android.testsupport;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TestUrns {

    public static List<Urn> createTrackUrns(Long... ids) {
        return Lists.transform(new ArrayList<>(Arrays.asList(ids)), Urn::forTrack);
    }

    private TestUrns() {
        // no instances
    }
}
