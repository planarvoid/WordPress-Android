package com.soundcloud.android.utils;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.strings.Strings;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class Urns {

    public static final Function<Urn, Long> TO_ID = new Function<Urn, Long>() {
        @Override
        public Long apply(Urn urn) {
            return urn.getNumericId();
        }
    };
    
    private static final Function<Urn, String> URN_TO_STRING = new Function<Urn, String>() {
        @Override
        public String apply(Urn input) {
            return input.toString();
        }
    };

    public static Function<Urn, String> toStringFunc() {
        return URN_TO_STRING;
    }

    public static ArrayList<String> toString(List<Urn> urns){
        final ArrayList<String> urnStrings = new ArrayList<>(urns.size());
        for (Urn urn : urns) {
            urnStrings.add(urn.toString());
        }
        return urnStrings;
    }

    public static String toJoinedIds(List<Urn> urns, String delimiter){
        final ArrayList<String> idStrings = new ArrayList<>(urns.size());
        for (Urn urn : urns) {
            idStrings.add(String.valueOf(urn.getNumericId()));
        }
        return Strings.joinOn(delimiter).join(idStrings);
    }

    @NonNull
    public static List<Long> toIds(List<Urn> urns) {
        return Lists.transform(urns, TO_ID);
    }

    private Urns() {
        // no instances
    }
}
