package com.soundcloud.android.experiments;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.utils.DeviceHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class ExperimentOperationsTest {

    ExperimentOperations operations;

    @Mock
    private ExperimentStorage experimentStorage;

    @Mock
    private RxHttpClient rxHttpClient;

    @Mock
    private ActiveExperiments activeExperiments;

    @Mock
    private DeviceHelper deviceHelper;


    @Before
    public void setUp() throws Exception {
        operations = new ExperimentOperations(experimentStorage, rxHttpClient, activeExperiments,
                deviceHelper);
        when(deviceHelper.getUniqueDeviceID()).thenReturn("device1");
    }

    @Test
    public void shouldGetEmptyAssignmentsIfHasNotInitialised() {
        Assignment assignment = operations.getAssignment();

        expect(assignment.isEmpty()).toBeTrue();
    }

    @Test
    public void shouldGetEmptyAssignmentIfNoAssignmentIsStored() throws Exception {
        when(experimentStorage.loadAssignmentAsync()).thenReturn(Observable.<Assignment>empty());
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());

        operations.loadAssignment();
        Assignment assignment = operations.getAssignment();

        expect(assignment.isEmpty()).toBeTrue();
    }

    @Test
    public void shouldGetAssignmentIfAssigmentIsStored() throws Exception {
        Assignment assignment = TestHelper.getModelFactory().createModel(Assignment.class);
        when(experimentStorage.loadAssignmentAsync()).thenReturn(Observable.from(assignment));
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());

        operations.loadAssignment();
        Assignment loadedAssignment = operations.getAssignment();

        expect(loadedAssignment.getLayers()).toEqual(assignment.getLayers());
    }

    @Test
    public void shouldFetchNewAssigmentAndSaveToFileOnInit() throws Exception {
        Assignment assignment = TestHelper.getModelFactory().createModel(Assignment.class);
        Observable<Assignment> observable = Observable.from(assignment);
        when(experimentStorage.loadAssignmentAsync()).thenReturn(Observable.<Assignment>empty());
        when(activeExperiments.getRequestLayers()).thenReturn(new String[]{ "android-ui" });
        when(rxHttpClient.<Assignment>fetchModels(any(APIRequest.class))).thenReturn(observable);

        operations.loadAssignment();

        verify(experimentStorage).storeAssignment(eq(assignment));
    }

    @Test
    public void shouldGenerateTrackingParametersMapForActiveExperiments() throws Exception {
        Assignment assignment = TestHelper.getModelFactory().createModel(Assignment.class);
        when(experimentStorage.loadAssignmentAsync()).thenReturn(Observable.from(assignment));
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        when(activeExperiments.isActive(anyInt())).thenReturn(true);

        operations.loadAssignment();

        Map<String, Integer> params = operations.getTrackingParams();

        expect(params.containsKey("exp_android-ui")).toBeTrue();
        expect(params.get("exp_android-ui")).toEqual(3);

        expect(params.containsKey("exp_android-listen")).toBeTrue();
        expect(params.get("exp_android-listen")).toEqual(9);
    }

    @Test
    public void shouldNotGenerateTrackingParametersForExperimentsThatAreNotRunning() throws Exception {
        Assignment assignment = TestHelper.getModelFactory().createModel(Assignment.class);
        when(experimentStorage.loadAssignmentAsync()).thenReturn(Observable.from(assignment));
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        when(activeExperiments.isActive(5)).thenReturn(true);


        operations.loadAssignment();

        Map<String, Integer> params = operations.getTrackingParams();

        expect(params.containsKey("exp_android-ui")).toBeTrue();
        expect(params.containsKey("exp_android-listen")).toBeFalse();
    }

    @Test
    public void shouldNotLoadAssignmentsIfDeviceIdIsNull() {
        when(deviceHelper.getUniqueDeviceID()).thenReturn("");
        operations.loadAssignment();
        verifyZeroInteractions(rxHttpClient, experimentStorage);
    }

}
