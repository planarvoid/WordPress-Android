package com.soundcloud.android.storage;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;

import android.content.Context;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class JsonFileStorage {

    private static final String UTF_8 = "UTF8";

    private final Context context;
    private final JsonTransformer jsonTransformer;

    @Inject
    JsonFileStorage(Context context,
                    JsonTransformer jsonTransformer) {
        this.context = context;
        this.jsonTransformer = jsonTransformer;
    }

    public boolean writeToFile(String fileName, Object toStore) {
        FileOutputStream fileOutputStream = null;
        try {
            String json = jsonTransformer.toJson(toStore);
            fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fileOutputStream.write(json.getBytes(UTF_8));
        } catch (ApiMapperException | IOException e) {
            ErrorUtils.handleSilentException("Failed to store data to file: " + fileName, e);
            return false;
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                ErrorUtils.handleSilentException("Failed closing file: " + fileName, e);
                return false;
            }
        }

        return true;
    }

    public <T> Observable<T> readFromFile(String fileName, TypeToken<T> typeToken) {
        if (Arrays.asList(context.fileList()).contains(fileName)) {
            return readStringFromFile(fileName)
                    .flatMap(jsonString -> {
                        try {
                            return Observable.just(jsonTransformer.fromJson(jsonString, typeToken));
                        } catch (IOException | ApiMapperException e) {
                            ErrorUtils.handleSilentException("Error reading from file: " + fileName, e);
                            return Observable.error(e);
                        }
                    });
        }

        return Observable.empty();
    }


    private Observable<String> readStringFromFile(String fileName) {
        return Observable.fromCallable(() -> {
            FileInputStream fileInputStream = context.openFileInput(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder jsonStringBuilder = new StringBuilder();

            try {
                String line = bufferedReader.readLine();
                while (line != null) {
                    jsonStringBuilder.append(line);
                    line = bufferedReader.readLine();
                }
            } finally {
                fileInputStream.close();
                inputStreamReader.close();
                bufferedReader.close();
            }

            return jsonStringBuilder.toString();
        });
    }
}
