package com.soundcloud.android.configuration.experiments;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.configuration.experiments.ExperimentStorage.AssignmentJsonTransformer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Test;

import java.io.IOException;

public class AssignmentJsonTransformerTest extends AndroidUnitTest {

    private AssignmentJsonTransformer transformer = new AssignmentJsonTransformer();

    @Test
    public void fromJson() throws IOException, ApiMapperException {
        final String json = "[{\"layer_name\" : \"android_listening\", \"experiment_id\" : 123, \"experiment_name\" : \"android_play_history\", \"variant_id\" : 456, \"variant_name\" : \"collection_above\"}]";
        final Assignment assignment = transformer.fromJson(json, TypeToken.of(Assignment.class));

        assertThat(assignment.getLayers()).hasSize(1);

        final Layer layer = assignment.getLayers().get(0);
        assertThat(layer.getExperimentId()).isEqualTo(123);
        assertThat(layer.getExperimentName()).isEqualTo("android_play_history");
        assertThat(layer.getLayerName()).isEqualTo("android_listening");
        assertThat(layer.getVariantId()).isEqualTo(456);
        assertThat(layer.getVariantName()).isEqualTo("collection_above");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fromJsonThrowExceptionWhenTypeDiffersFromAssignment() throws IOException, ApiMapperException {
        final String json = "[{\"layer_name\" : \"android_listening\", \"experiment_id\" : 123, \"experiment_name\" : \"android_play_history\", \"variant_id\" : 456, \"variant_name\" : \"collection_above\"}]";
        transformer.fromJson(json, TypeToken.of(Layer.class));
    }

    @Test
    public void testName() throws ApiMapperException, IOException {
        final Layer expectedLayer = new Layer("collection_above", 123, "android_play_history", 456, "collection_above");
        final Assignment expectedAssignment = new Assignment(singletonList(expectedLayer));

        final String json = transformer.toJson(expectedAssignment);
        final Assignment actual = transformer.fromJson(json, TypeToken.of(Assignment.class));


        assertThat(actual.getLayers()).hasSize(1);

        final Layer layer = actual.getLayers().get(0);
        assertThat(layer.getExperimentId()).isEqualTo(layer.getExperimentId());
        assertThat(layer.getExperimentName()).isEqualTo(layer.getExperimentName());
        assertThat(layer.getLayerName()).isEqualTo(layer.getLayerName());
        assertThat(layer.getVariantId()).isEqualTo(layer.getVariantId());
        assertThat(layer.getVariantName()).isEqualTo(layer.getVariantName());
    }
}
