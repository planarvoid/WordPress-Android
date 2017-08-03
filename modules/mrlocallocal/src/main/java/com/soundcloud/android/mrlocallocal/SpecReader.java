package com.soundcloud.android.mrlocallocal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.CharStreams;
import com.soundcloud.android.mrlocallocal.data.Spec;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

class SpecReader {

    private final Context context;
    private final ObjectMapper yamlMapper;

    SpecReader(Context context) {
        this.context = context;
        yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    Spec readSpec(String specName, Map<String, String> stringSubstitutions) throws IOException {
        String input = readFileToString(specName);
        input = performSubstitutions(input, stringSubstitutions);
        return yamlMapper.readValue(input, Spec.class);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String readFileToString(String filename) throws IOException {
        InputStream inputStream = context.getResources().getAssets().open(filename);

        String fileContents;
        try (final Reader reader = new InputStreamReader(inputStream)) {
            fileContents = CharStreams.toString(reader);
        }

        return fileContents;
    }

    private String performSubstitutions(String input, Map<String, String> stringSubstitutions) {
        if (!stringSubstitutions.isEmpty()) {
            for (Map.Entry<String, String> stringSubstitution : stringSubstitutions.entrySet()) {
                String findRegex, replace;
                if ((findRegex = stringSubstitution.getKey()) != null && (replace = stringSubstitution.getValue()) != null) {
                    input = input.replaceAll(findRegex, replace);
                }
            }
        }
        return input;
    }
}
