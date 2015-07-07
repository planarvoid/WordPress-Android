package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.testsupport.PlatformUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ExperimentStorageTest extends PlatformUnitTest {

    private static final String JSON = "{ \"key\": \"value\" }";
    private static final Assignment ASSIGNMENT = ModelFixtures.create(Assignment.class);

    @Mock private JsonTransformer jsonTransformer;

    private ExperimentStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new ExperimentStorage(Schedulers.immediate(), context(), jsonTransformer);
    }

    @Test
    public void loadAssignmentIfFileExists() throws IOException, ApiMapperException {
        writeAssignment(getAssignmentFile(), JSON);
        when(jsonTransformer.fromJson(eq(JSON), any(TypeToken.class))).thenReturn(ASSIGNMENT);

        TestSubscriber<Assignment> subscriber = new TestSubscriber<>();
        storage.readAssignment().subscribe(subscriber);
        assertThat(subscriber.getOnNextEvents()).containsExactly(ASSIGNMENT);
    }

    @Test
    public void loadEmptyAssignmentIfNoFileExists() {
        IOUtils.deleteFile(getAssignmentFile());

        TestSubscriber<Assignment> subscriber = new TestSubscriber<>();
        storage.readAssignment().subscribe(subscriber);
        assertThat(subscriber.getOnNextEvents()).containsExactly(Assignment.empty());
    }

    @Test
    public void returnEmptyAssignmentWhenDeserializationFailed() throws IOException, ApiMapperException {
        when(jsonTransformer.fromJson(eq(JSON), any(TypeToken.class))).thenThrow(new ApiMapperException("Fake exception"));

        TestSubscriber<Assignment> subscriber = new TestSubscriber<>();
        storage.readAssignment().subscribe(subscriber);
        assertThat(subscriber.getOnNextEvents()).containsExactly(Assignment.empty());
    }

    @Test
    public void deleteFileWhenDeserializationFailed() throws IOException, ApiMapperException {
        writeAssignment(getAssignmentFile(), JSON);
        when(jsonTransformer.fromJson(eq(JSON), any(TypeToken.class))).thenThrow(new ApiMapperException("Fake exception"));

        storage.readAssignment().subscribe(new TestSubscriber<Assignment>());

        assertThat(getAssignmentFile().exists()).isFalse();
    }

    @Test
    public void saveAssignmentToFile() throws IOException, ApiMapperException {
        when(jsonTransformer.toJson(ASSIGNMENT)).thenReturn(JSON);

        storage.storeAssignment(ASSIGNMENT);

        String writtenAssignmentJson = readAssignment(getAssignmentFile());
        assertThat(getAssignmentFile().exists()).isTrue();
        assertThat(writtenAssignmentJson).isEqualTo(JSON);
    }

    private File getAssignmentFile() {
        return new File(context().getFilesDir(), ".assignment");
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
