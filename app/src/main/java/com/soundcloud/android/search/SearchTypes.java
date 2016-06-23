package com.soundcloud.android.search;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Predicate;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class SearchTypes {

    private final List<SearchType> available;

    @Inject
    SearchTypes(final FeatureFlags featureFlags) {
        this.available = Lists.newArrayList(Iterables.filter(Arrays.asList(SearchType.values()),
                                                             new Predicate<SearchType>() {
                                                                 @Override
                                                                 public boolean apply(@Nullable SearchType input) {
                                                                     return input != SearchType.ALBUMS || featureFlags.isEnabled(
                                                                             Flag.ALBUMS);
                                                                 }
                                                             }));
    }

    List<SearchType> available() {
        return available;
    }

    SearchType get(int position) {
        return available.get(position);
    }
}
