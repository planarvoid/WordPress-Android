package com.soundcloud.android.experiments;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.IOUtils;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Subscriber;
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

    private ExperimentStorage storage;

    private Context context = Robolectric.application.getApplicationContext();

    @Mock
    private JsonTransformer jsonTransformer;

    @Mock
    private Subscriber subscriber;

    @Before
    public void setUp() throws Exception {
        storage = new ExperimentStorage(Schedulers.immediate(), context, jsonTransformer);
    }

    @Test
    public void shouldEmitAssigmentIfAssignmentFileExistsOnLoadAsync() throws Exception {
        Assignment assignment = new Assignment();
        writeAssignment(getAssignmentHandle(), JSON);
        when(jsonTransformer.fromJson(eq(JSON), any(TypeToken.class))).thenReturn(assignment);

        expect(storage.loadAssignmentAsync().toBlockingObservable().first()).not.toBeNull();
    }

    @Test
    public void shouldLoadAssignmentFromFile() throws Exception {
        Assignment assignment = new Assignment();
        writeAssignment(getAssignmentHandle(), JSON);
        when(jsonTransformer.fromJson(eq(JSON), any(TypeToken.class))).thenReturn(assignment);

        storage.loadAssignment();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jsonTransformer).fromJson(captor.capture(), any(TypeToken.class));
        expect(captor.getValue()).toEqual(JSON);
    }

    @Test
    public void shouldSaveAssignmentToFile() throws Exception {
        Assignment assignment = new Assignment();
        when(jsonTransformer.toJson(assignment)).thenReturn(JSON);

        storage.storeAssignment(assignment);

        String writtenAssignmentJson = readAssignment(getAssignmentHandle());
        expect(getAssignmentHandle().exists()).toBeTrue();
        expect(writtenAssignmentJson).toEqual(JSON);
    }

    private File getAssignmentHandle() {
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
