package com.soundcloud.android.experiments;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.IOUtils;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@RunWith(SoundCloudTestRunner.class)
public class ExperimentStorageTest {

    private static final String JSON = "{ \"key\": \"value\" }";
    private static final Assignment ASSIGNMENT = ModelFixtures.create(Assignment.class);

    @Mock private JsonTransformer jsonTransformer;

    private ExperimentStorage storage;
    private Context context = Robolectric.application.getApplicationContext();

    @Before
    public void setUp() throws Exception {
        storage = new ExperimentStorage(Schedulers.immediate(), context, jsonTransformer);
    }

    @Test
    public void loadAssignmentIfFileExists() throws IOException, ApiMapperException {
        writeAssignment(getAssignmentFile(), JSON);
        when(jsonTransformer.fromJson(eq(JSON), any(TypeToken.class))).thenReturn(ASSIGNMENT);

        TestSubscriber<Assignment> subscriber = new TestSubscriber<>();
        storage.readAssignment().subscribe(subscriber);
        expect(subscriber.getOnNextEvents()).toContainExactly(ASSIGNMENT);
    }

    @Test
    public void loadEmptyAssignmentIfNoFileExists() {
        IOUtils.deleteFile(getAssignmentFile());

        TestSubscriber<Assignment> subscriber = new TestSubscriber<>();
        storage.readAssignment().subscribe(subscriber);
        expect(subscriber.getOnNextEvents()).toContainExactly(Assignment.empty());
    }

    @Test
    public void returnEmptyAssignmentWhenDeserializationFailed() throws IOException, ApiMapperException {
        when(jsonTransformer.fromJson(eq(JSON), any(TypeToken.class))).thenThrow(new ApiMapperException("Fake exception"));

        TestSubscriber<Assignment> subscriber = new TestSubscriber<>();
        storage.readAssignment().subscribe(subscriber);
        expect(subscriber.getOnNextEvents()).toContainExactly(Assignment.empty());
    }

    @Test
    public void deleteFileWhenDeserializationFailed() throws IOException, ApiMapperException {
        writeAssignment(getAssignmentFile(), JSON);
        when(jsonTransformer.fromJson(eq(JSON), any(TypeToken.class))).thenThrow(new ApiMapperException("Fake exception"));

        storage.readAssignment().subscribe(new TestSubscriber<Assignment>());

        expect(getAssignmentFile().exists()).toBeFalse();
    }

    @Test
    public void saveAssignmentToFile() throws IOException, ApiMapperException {
        when(jsonTransformer.toJson(ASSIGNMENT)).thenReturn(JSON);

        storage.storeAssignment(ASSIGNMENT);

        String writtenAssignmentJson = readAssignment(getAssignmentFile());
        expect(getAssignmentFile().exists()).toBeTrue();
        expect(writtenAssignmentJson).toEqual(JSON);
    }

    private File getAssignmentFile() {
        return new File(context.getFilesDir(), ".assignment");
    }

    private String readAssignment(File file) throws IOException {
        return IOUtils.readInputStream(new FileInputStream(file));
    }

    private void writeAssignment(File file, String json) throws IOException {
        OutputStream output = null;
        try  {
            output = new FileOutputStream(file);
            output.write(json.getBytes());
        } finally {
            IOUtils.close(output);
        }
    }

}
