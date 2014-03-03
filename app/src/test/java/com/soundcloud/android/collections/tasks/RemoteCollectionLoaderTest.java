package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.api.Request;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class RemoteCollectionLoaderTest {

    private RemoteCollectionLoader<ScResource> remoteCollectionLoader;
    @Mock
    private PublicCloudAPI publicCloudApi;
    @Mock
    private CollectionParams<ScResource> parameters;
    @Mock
    private Request request;
    @Mock
    private CollectionHolder<ScResource> collectionHolder;
    @Mock
    private TrackStorage trackStorage;

    @Before
    public void setup() throws IOException {
        initMocks(this);
        remoteCollectionLoader = new RemoteCollectionLoader<ScResource>(trackStorage);

        stub(parameters.getRequest()).toReturn(request);
        stub(publicCloudApi.readCollection(request)).toReturn(collectionHolder);
    }


    @Test
    public void shouldRemoveUnknownResourcesFromCollectionHolder() throws IOException {
        remoteCollectionLoader.load(publicCloudApi,parameters);
        verify(collectionHolder).removeUnknownResources();
    }

    @Test
    public void shouldReturnAResultWithInformationFromTheCollectionHolder(){
        List<ScResource> collection = new ArrayList<ScResource>();
        String nextHref = "linkylink";
        when(collectionHolder.getCollection()).thenReturn(collection);
        when(collectionHolder.getNextHref()).thenReturn(nextHref);
        when(collectionHolder.moreResourcesExist()).thenReturn(true);

        ReturnData<ScResource> responseData = remoteCollectionLoader.load(publicCloudApi, parameters);

        assertThat(responseData.newItems, is(collection));
        assertThat(responseData.success, is(true));
        assertThat(responseData.keepGoing, is(true));
        assertThat(responseData.nextHref, is(nextHref));
        assertThat(responseData.responseCode, is(200));
    }

    @Test
    public void shouldReturnAResultThatSpecificAnErrorIfIOExceptionOccurs() throws IOException {
        when(parameters.isRefresh()).thenReturn(true);
        when(publicCloudApi.readCollection(request)).thenThrow(IOException.class);
        ReturnData<ScResource> responseData = remoteCollectionLoader.load(publicCloudApi, parameters);
        assertThat(responseData.success, is(false));
        assertThat(responseData.wasRefresh, is(true));
        assertThat(responseData.responseCode, is(EmptyListView.Status.CONNECTION_ERROR));
    }

    @Test
    public void shouldStoreTracksFromCollection() throws CreateModelException {
        List<ScResource> collection = new ArrayList<ScResource>();
        final Track track1 = TestHelper.getModelFactory().createModel(Track.class);
        final Track track2 = TestHelper.getModelFactory().createModel(Track.class);

        collection.add(TestHelper.getModelFactory().createModel(User.class));
        collection.add(track1);
        collection.add(TestHelper.getModelFactory().createModel(User.class));
        collection.add(track2);

        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(trackStorage.storeCollectionAsync(anyCollection())).thenReturn(observable);

        when(collectionHolder.getCollection()).thenReturn(collection);
        remoteCollectionLoader.load(publicCloudApi, parameters);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(trackStorage).storeCollectionAsync((Collection<Track>) captor.capture());
        expect(captor.getValue()).toContainExactly(track1, track2);

        expect(observable.subscribedTo()).toBeTrue();
    }
}
