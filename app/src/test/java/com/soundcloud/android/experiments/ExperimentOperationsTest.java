package com.soundcloud.android.experiments;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
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
    ExperimentStorage experimentStorage;

    @Mock
    RxHttpClient rxHttpClient;

    @Before
    public void setUp() throws Exception {
        operations = new ExperimentOperations(experimentStorage, rxHttpClient);
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

        operations.loadAssignment("device1");
        Assignment assignment = operations.getAssignment();

        expect(assignment.isEmpty()).toBeTrue();
    }

    @Test
    public void shouldGetAssignmentIfAssigmentIsStored() throws Exception {
        Assignment assignment = TestHelper.getModelFactory().createModel(Assignment.class);
        when(experimentStorage.loadAssignmentAsync()).thenReturn(Observable.from(assignment));
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());

        operations.loadAssignment("device1");
        Assignment loadedAssignment = operations.getAssignment();

        expect(loadedAssignment.getLayers()).toEqual(assignment.getLayers());
    }

    @Test
    public void shouldFetchNewAssigmentAndSaveToFileOnInit() throws Exception {
        Assignment assignment = TestHelper.getModelFactory().createModel(Assignment.class);
        Observable<Assignment> observable = Observable.from(assignment);
        when(experimentStorage.loadAssignmentAsync()).thenReturn(Observable.<Assignment>empty());
        when(rxHttpClient.<Assignment>fetchModels(any(APIRequest.class))).thenReturn(observable);

        operations.loadAssignment("device1");

        verify(experimentStorage).storeAssignment(eq(assignment));
    }

    @Test
    public void shouldPopulateTrackingParametersMapFromAssignment() throws Exception {
        Assignment assignment = TestHelper.getModelFactory().createModel(Assignment.class);
        when(experimentStorage.loadAssignmentAsync()).thenReturn(Observable.from(assignment));
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        operations.loadAssignment("device1");

        Map<String, Integer> params = operations.getTrackingParams();

        expect(params.containsKey("exp_android-ui")).toBeTrue();
        expect(params.get("exp_android-ui")).toEqual(1);

        expect(params.containsKey("exp_android-listen")).toBeTrue();
        expect(params.get("exp_android-listen")).toEqual(2);
    }

}
