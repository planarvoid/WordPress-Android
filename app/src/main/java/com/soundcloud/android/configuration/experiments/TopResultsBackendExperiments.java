package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;

import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class TopResultsBackendExperiments {

    static final String VARIANT_CONTROL_1 = "control1";
    static final String VARIANT_CONTROL_2 = "control2";
    static final String VARIANT_TOP_RESULT = "topresult";
    static final String VARIANT_FIXED_BUCKETS = "fixed_buckets";
    static final String VARIANT_VARIABLE_BUCKETS = "variable_buckets";
    static final String VARIANT_TOP_RESULT_MIXED_LIST = "topresult_mixed_list";

    private static final List<String> VARIANTS = Arrays.asList(VARIANT_CONTROL_1,
                                                               VARIANT_CONTROL_2,
                                                               VARIANT_TOP_RESULT,
                                                               VARIANT_FIXED_BUCKETS,
                                                               VARIANT_VARIABLE_BUCKETS,
                                                               VARIANT_TOP_RESULT_MIXED_LIST);

    private static final List<String> TOP_RESULTS_ENABLED_VARIANTS = Lists.newArrayList(VARIANT_TOP_RESULT, VARIANT_FIXED_BUCKETS, VARIANT_VARIABLE_BUCKETS);

    private final ExperimentOperations experimentOperations;

    @Inject
    TopResultsBackendExperiments(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    boolean topResultsEnabled() {
        final Optional<String> variant = getVariant();
        return variant.isPresent() && TOP_RESULTS_ENABLED_VARIANTS.contains(variant.get());
    }

    private Optional<String> getVariant() {
        return experimentOperations.getOptionalExperimentVariant(OrderOfBucketsExperiment.CONFIGURATION)
                                   .or(experimentOperations.getOptionalExperimentVariant(MoreResultsExperiment.CONFIGURATION))
                                   .or(experimentOperations.getOptionalExperimentVariant(CombinedImprovementsExperiment.CONFIGURATION));
    }

    @ActiveExperiment
    public static class OrderOfBucketsExperiment {
        static final String NAME = "order_of_buckets_top_results_android";

        public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
                .fromName(LISTENING_LAYER, NAME, VARIANTS);
    }

    @ActiveExperiment
    public static class MoreResultsExperiment {
        static final String NAME = "more_results_top_results_android";

        public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
                .fromName(LISTENING_LAYER, NAME, VARIANTS);
    }

    @ActiveExperiment
    public static class CombinedImprovementsExperiment {
        static final String NAME = "combined_improvements_top_results_android";

        public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
                .fromName(LISTENING_LAYER, NAME, VARIANTS);
    }
}