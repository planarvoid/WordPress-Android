package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;

import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import java.util.List;

public class TopResultsBackendExperiments {

    static final String VARIANT_CONTROL_1 = "control1";
    static final String VARIANT_CONTROL_2 = "control2";
    static final String VARIANT_TOP_RESULT = "topresult";
    static final String VARIANT_FIXED_BUCKETS = "fixed_buckets";
    static final String VARIANT_VARIABLE_BUCKETS = "variable_buckets";
    static final String VARIANT_TOP_RESULT_MIXED_LIST = "topresult_mixed_list";

    private static final List<String> TOP_RESULTS_ENABLED_VARIANT_NAMES = Lists.newArrayList(VARIANT_TOP_RESULT, VARIANT_FIXED_BUCKETS, VARIANT_VARIABLE_BUCKETS);

    private final ExperimentOperations experimentOperations;

    @Inject
    TopResultsBackendExperiments(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    boolean topResultsEnabled() {
        final Optional<String> variantName = getVariantName();
        return variantName.isPresent() && TOP_RESULTS_ENABLED_VARIANT_NAMES.contains(variantName.get());
    }

    private Optional<String> getVariantName() {
        return experimentOperations.getOptionalExperimentVariant(OrderOfBucketsExperiment.CONFIGURATION)
                                   .or(experimentOperations.getOptionalExperimentVariant(MoreResultsExperiment.CONFIGURATION))
                                   .or(experimentOperations.getOptionalExperimentVariant(CombinedImprovementsExperiment.CONFIGURATION));
    }

    @ActiveExperiment
    public static class OrderOfBucketsExperiment {
        static final String NAME = "order_of_buckets_top_results_android";
        static final int ID = 209;

        public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
                .fromNamesAndIds(LISTENING_LAYER, NAME, ID, Lists.newArrayList(
                        Pair.of(VARIANT_CONTROL_1, 570),
                        Pair.of(VARIANT_CONTROL_2, 600),
                        Pair.of(VARIANT_TOP_RESULT, 571),
                        Pair.of(VARIANT_FIXED_BUCKETS, 572),
                        Pair.of(VARIANT_VARIABLE_BUCKETS, 573),
                        Pair.of(VARIANT_TOP_RESULT_MIXED_LIST, 574)
                ));
    }

    @ActiveExperiment
    public static class MoreResultsExperiment {
        static final String NAME = "more_results_top_results_android";
        static final int ID = 213;

        public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
                .fromNamesAndIds(LISTENING_LAYER, NAME, ID, Lists.newArrayList(
                        Pair.of(VARIANT_CONTROL_1, 590),
                        Pair.of(VARIANT_CONTROL_2, 604),
                        Pair.of(VARIANT_TOP_RESULT, 591),
                        Pair.of(VARIANT_FIXED_BUCKETS, 592),
                        Pair.of(VARIANT_VARIABLE_BUCKETS, 593),
                        Pair.of(VARIANT_TOP_RESULT_MIXED_LIST, 594)
                ));
    }

    @ActiveExperiment
    public static class CombinedImprovementsExperiment {
        static final String NAME = "combined_improvements_top_results_android";
        static final int ID = 214;

        public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
                .fromNamesAndIds(LISTENING_LAYER, NAME, ID, Lists.newArrayList(
                        Pair.of(VARIANT_CONTROL_1, 595),
                        Pair.of(VARIANT_CONTROL_2, 605),
                        Pair.of(VARIANT_TOP_RESULT, 596),
                        Pair.of(VARIANT_FIXED_BUCKETS, 597),
                        Pair.of(VARIANT_VARIABLE_BUCKETS, 598),
                        Pair.of(VARIANT_TOP_RESULT_MIXED_LIST, 599)
                ));
    }
}
