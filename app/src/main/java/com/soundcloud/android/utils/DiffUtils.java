package com.soundcloud.android.utils;

import com.soundcloud.android.model.Urn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class DiffUtils {
    public static List<Urn> minus(List<Urn> op1, List<Urn> op2) {
        final List<Urn> result = new ArrayList<>(op1);
        result.removeAll(op2);
        return result;
    }

    public static <T> List<T> deduplicate(Collection<T> source) {
        final LinkedHashSet<T> deduplicate = new LinkedHashSet<>();
        deduplicate.addAll(source);
        return new ArrayList<>(deduplicate);
    }
}
