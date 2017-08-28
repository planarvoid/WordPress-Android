package com.soundcloud.groupie;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class ExperimentConfiguration {

    /**
     * @deprecated Using only names for experiments / variants is error-prone since is possible to have 2 experiments configured with the same names / variant names.
     * Use {@link #fromNamesAndIds(String, String, int, List)} and include the ids provided by the Roadie user interface.
     */
    @Deprecated
    public static ExperimentConfiguration fromName(String layerName, String experimentName, List<String> variantNames) {
        return builder()
                .layerName(layerName)
                .experimentName(experimentName)
                .experimentId(Optional.absent())
                .variants(Lists.transform(variantNames, name -> Pair.of(name, Optional.absent())))
                .build();
    }

    public static ExperimentConfiguration fromNamesAndIds(String layerName, String experimentName, int experimentId, List<Pair<String, Integer>> variants) {
        return builder()
                .layerName(layerName)
                .experimentName(experimentName)
                .experimentId(Optional.of(experimentId))
                .variants(Lists.transform(variants, pair -> Pair.of(pair.first(), Optional.of(pair.second()))))
                .build();
    }

    public static Builder builder() {
        return new AutoValue_ExperimentConfiguration.Builder();
    }

    public abstract String getLayerName();

    public abstract String getExperimentName();

    public abstract Optional<Integer> getExperimentId();

    public abstract List<Pair<String, Optional<Integer>>> getVariants();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder layerName(String layer);

        public abstract Builder experimentName(String name);

        public abstract Builder experimentId(Optional<Integer> experimentId);

        public abstract Builder variants(List<Pair<String, Optional<Integer>>> variants);

        public abstract ExperimentConfiguration build();
    }
}
