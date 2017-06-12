package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.java.strings.Strings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ExperimentStorage {

    private static final String ASSIGNMENT_FILE_NAME = ".assignment";

    private final JsonTransformer jsonTransformer;
    private final File file;

    @Inject
    ExperimentStorage(Context context, AssignmentJsonTransformer jsonTransformer) {
        this.jsonTransformer = jsonTransformer;
        this.file = new File(context.getFilesDir(), ASSIGNMENT_FILE_NAME);
    }

    void storeAssignment(Assignment assignment) {
        try {
            String json = jsonTransformer.toJson(assignment);
            IOUtils.writeFileFromString(file, json);
        } catch (ApiMapperException e) {
            ErrorUtils.handleThrowable(e, getClass());
        }
    }

    Assignment readAssignment() {
        return file.exists() ? readAssignmentFile() : Assignment.empty();
    }

    private Assignment readAssignmentFile() {
        String json = Strings.EMPTY;
        try {
            json = IOUtils.readInputStream(new FileInputStream(file));
            return jsonTransformer.fromJson(json, TypeToken.of(Assignment.class));
        } catch (IOException e) {
            ErrorUtils.handleSilentException(e);
            return Assignment.empty();
        } catch (ApiMapperException e) {
            // see https://www.crashlytics.com/soundcloudandroid/android/apps/com.soundcloud.android/issues/5452b652e3de5099ba2b4fea
            ErrorUtils.handleSilentException(new IllegalStateException("Failed parsing assignment; json = " + json, e));
            IOUtils.deleteFile(file);
            return Assignment.empty();
        }
    }

    static class AssignmentJsonTransformer implements JsonTransformer {

        private static final String LAYER_NAME = "layer_name";
        private static final String EXPERIMENT_ID = "experiment_id";
        private static final String EXPERIMENT_NAME = "experiment_name";
        private static final String VARIANT_ID = "variant_id";
        private static final String VARIANT_NAME = "variant_name";

        @Inject
        public AssignmentJsonTransformer() {
        }

        @Override
        public <T> T fromJson(String json, TypeToken<T> classToTransformTo) throws IOException, ApiMapperException {
            checkArgument(classToTransformTo.getRawType().equals(Assignment.class));

            try {
                return jsonToAssignment(json);
            } catch (JSONException e) {
                throw new ApiMapperException(e);
            }
        }

        private <T> T jsonToAssignment(String json) throws JSONException {
            final JSONArray experiments = new JSONArray(json);
            final List<Layer> layers = new ArrayList<>(experiments.length());

            for (int i = 0; i < experiments.length(); i++) {
                layers.add(jsonToLayer(experiments.getJSONObject(i)));
            }
            return (T) new Assignment(layers);
        }

        private Layer jsonToLayer(JSONObject jsonObject) throws JSONException {
            return new Layer(
                    jsonObject.getString(LAYER_NAME),
                    jsonObject.getInt(EXPERIMENT_ID),
                    jsonObject.getString(EXPERIMENT_NAME),
                    jsonObject.getInt(VARIANT_ID),
                    jsonObject.getString(VARIANT_NAME)
            );
        }

        @Override
        public String toJson(Object source) throws ApiMapperException {
            checkArgument(source.getClass().equals(Assignment.class));

            try {
                return assignmentToJson((Assignment) source).toString();
            } catch (JSONException e) {
                throw new ApiMapperException(e);
            }
        }

        private JSONArray assignmentToJson(Assignment assignment) throws JSONException {
            final JSONArray jsonArray = new JSONArray();
            for (Layer layer : assignment.getLayers()) {
                jsonArray.put(layerToJson(layer));
            }
            return jsonArray;
        }

        @NonNull
        private JSONObject layerToJson(Layer layer) throws JSONException {
            return new JSONObject()
                    .put(LAYER_NAME, layer.getLayerName())
                    .put(EXPERIMENT_ID, layer.getExperimentId())
                    .put(EXPERIMENT_NAME, layer.getExperimentName())
                    .put(VARIANT_ID, layer.getVariantId())
                    .put(VARIANT_NAME, layer.getVariantName());
        }

    }
}
