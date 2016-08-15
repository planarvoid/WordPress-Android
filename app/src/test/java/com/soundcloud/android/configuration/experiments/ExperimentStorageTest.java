package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.configuration.experiments.ExperimentStorage.AssignmentJsonTransformer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ExperimentStorageTest extends AndroidUnitTest {

    private static final String JSON = "{ \"key\": \"value\" }";
    private static final Assignment ASSIGNMENT = ModelFixtures.create(Assignment.class);

    @Mock private AssignmentJsonTransformer jsonTransformer;

    private ExperimentStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new ExperimentStorage(context(), jsonTransformer);
    }

    @Test
    public void loadAssignmentIfFileExists() throws IOException, ApiMapperException {
        writeAssignment(getAssignmentFile(), JSON);
        when(jsonTransformer.fromJson(eq(JSON), any(TypeToken.class))).thenReturn(ASSIGNMENT);

        assertThat(storage.readAssignment()).isEqualTo(ASSIGNMENT);
    }

    @Test
    public void loadEmptyAssignmentIfNoFileExists() {
        IOUtils.deleteFile(getAssignmentFile());

        assertThat(storage.readAssignment()).isEqualTo(Assignment.empty());
    }

    @Test
    public void returnEmptyAssignmentWhenDeserializationFailed() throws IOException, ApiMapperException {
        when(jsonTransformer.fromJson(eq(JSON),
                                      any(TypeToken.class))).thenThrow(new ApiMapperException("Fake exception"));

        assertThat(storage.readAssignment()).isEqualTo(Assignment.empty());
    }

    @Test
    public void deleteFileWhenDeserializationFailed() throws IOException, ApiMapperException {
        writeAssignment(getAssignmentFile(), JSON);
        when(jsonTransformer.fromJson(eq(JSON),
                                      any(TypeToken.class))).thenThrow(new ApiMapperException("Fake exception"));

        storage.readAssignment();

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
        try {
            output = new FileOutputStream(file);
            output.write(json.getBytes());
        } finally {
            IOUtils.close(output);
        }
    }

}
