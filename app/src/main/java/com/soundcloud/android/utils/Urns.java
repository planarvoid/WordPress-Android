package com.soundcloud.android.utils;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Urns {

    public static final Function<Urn, Long> TO_ID = urn -> urn.getNumericId();

    public static final Function<Urn, String> TO_STRING = urn -> urn.toString();

    public static final Predicate<Urn> IS_NOT_TRACK = input -> !input.isTrack();

    public static final Predicate<Urn> VALID_URN_PREDICATE = urn -> urn.getNumericId() > 0;

    private static final Function<Urn, String> URN_TO_STRING = input -> input.toString();

    public static Function<Urn, String> toStringFunc() {
        return URN_TO_STRING;
    }

    public static ArrayList<String> toString(List<Urn> urns) {
        final ArrayList<String> urnStrings = new ArrayList<>(urns.size());
        for (Urn urn : urns) {
            urnStrings.add(urn.toString());
        }
        return urnStrings;
    }

    public static String toJoinedIds(List<Urn> urns, String delimiter) {
        final ArrayList<String> idStrings = new ArrayList<>(urns.size());
        for (Urn urn : urns) {
            idStrings.add(String.valueOf(urn.getNumericId()));
        }
        return Strings.joinOn(delimiter).join(idStrings);
    }

    public static Predicate<Urn> trackPredicate() {
        return urn -> urn.isTrack();
    }

    public static Predicate<Urn> playlistPredicate() {
        return urn -> urn.isPlaylist();
    }

    public static Optional<Urn> optionalFromNotSetUrn(Urn urn) {
        return Urn.NOT_SET.equals(urn) ? Optional.absent() : Optional.of(urn);
    }

    @NonNull
    public static List<Long> toIds(List<Urn> urns) {
        return Lists.transform(urns, TO_ID);
    }

    @NonNull
    public static Collection<Long> toIdsColl(Collection<Urn> urns) {
        return MoreCollections.transform(urns, TO_ID);
    }

    private Urns() {
        // no instances
    }

}
