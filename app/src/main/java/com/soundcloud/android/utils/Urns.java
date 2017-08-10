package com.soundcloud.android.utils;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Urns {

    public static final Function<Urn, Long> TO_ID = urn -> urn.getNumericId();

    public static final Predicate<Urn> IS_NOT_TRACK = input -> !input.isTrack();

    public static final Predicate<Urn> VALID_URN_PREDICATE = urn -> urn.getNumericId() > 0;

    public static ArrayList<String> toString(List<Urn> urns) {
        final ArrayList<String> urnStrings = new ArrayList<>(urns.size());
        for (Urn urn : urns) {
            urnStrings.add(urn.getContent());
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

    public static List<Long> extractIds(Iterable<Urn> urns, Optional<Predicate<Urn>> predicate) {
        final List<Long> ids = new ArrayList<>(Iterables.size(urns));

        for (Urn urn : urns) {
            if (!predicate.isPresent() || predicate.get().apply(urn)) {
                ids.add(urn.getNumericId());
            }
        }
        return ids;
    }

    private Urns() {
        // no instances
    }

    @Nullable
    public static Urn urnFromBundle(@Nullable Bundle bundle, String key) {
        if (bundle != null) {
            final String urn = bundle.getString(key);
            return urn != null ? new Urn(urn) : null;
        }
        return null;
    }

    public static Optional<Urn> optionalUrnFromBundle(@Nullable Bundle bundle, String key) {
        return Optional.fromNullable(urnFromBundle(bundle, key));
    }

    @Nullable
    public static List<Urn> urnsFromBundle(@Nullable Bundle bundle, String key) {
        if (bundle != null) {
            final List<String> urns = bundle.getStringArrayList(key);
            return urns != null ? Lists.transform(urns, Urn::new) : null;
        }
        return null;
    }

    @Nullable
    public static Urn urnFromIntent(@NonNull Intent intent, String key) {
        final String urn = intent.getStringExtra(key);
        return urn != null ? new Urn(urn) : null;
    }

    public static Optional<Urn> optionalUrnFromIntent(@NonNull Intent intent, String key) {
        return Optional.fromNullable(urnFromIntent(intent, key));
    }

    @Nullable
    public static List<Urn> urnsFromIntent(@NonNull Intent intent, String key) {
        final ArrayList<String> urns = intent.getStringArrayListExtra(key);
        if (urns != null) {
            return Lists.transform(urns, Urn::new);
        }
        return null;
    }

    @Nullable
    public static Urn urnFromParcel(@NonNull Parcel parcel) {
        final String urn = parcel.readString();
        return urn != null ? new Urn(urn) : null;
    }

    public static Optional<Urn> optionalUrnFromParcel(@NonNull Parcel parcel) {
        return Optional.fromNullable(urnFromParcel(parcel));
    }

    @Nullable
    public static List<Urn> urnsFromParcel(@NonNull Parcel parcel) {
        final ArrayList<String> urns = new ArrayList<>();
        parcel.readStringList(urns);
        return !urns.isEmpty() ? Lists.transform(urns, Urn::new) : null;
    }

    public static void writeToParcel(@NonNull Parcel dest, Urn urn) {
        dest.writeString(urn.getContent());

    }

    public static void writeToParcel(@NonNull Parcel dest, Optional<Urn> urn) {
        dest.writeString(urn.isPresent() ? urn.get().getContent() : null);
    }

    public static void writeToParcel(@NonNull Parcel dest, List<Urn> urns) {
        dest.writeStringList(urns != null ? Lists.transform(urns, Urn::getContent) : null);
    }

    public static void writeToBundle(@NonNull Bundle bundle, String key, @Nullable Urn urn) {
        bundle.putString(key, urn != null ? urn.getContent() : null);
    }

    public static void writeToBundle(@NonNull Bundle bundle, String key, Optional<Urn> optionalUrn) {
        optionalUrn.ifPresent(urn -> writeToBundle(bundle, key, urn));
    }

    public static Intent writeToIntent(@NonNull Intent intent, String key, Urn urn) {
        return intent.putExtra(key, urn.getContent());
    }

    public static Intent writeToIntent(@NonNull Intent intent, String key, Optional<Urn> optionalUrn) {
        optionalUrn.ifPresent(urn -> writeToIntent(intent, key, urn));
        return intent;
    }

    public static Intent writeToIntent(@NonNull Intent intent, String key, List<Urn> urns) {
        final ArrayList<String> stringUrns = new ArrayList<>(Lists.transform(urns, Urn::getContent));
        return intent.putStringArrayListExtra(key, stringUrns);
    }
}
