package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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

    private RemoteCollectionLoader<PublicApiResource> remoteCollectionLoader;
    @Mock
    private PublicCloudAPI publicCloudApi;
    @Mock
    private CollectionParams<PublicApiResource> parameters;
    @Mock
    private Request request;
    @Mock
    private CollectionHolder<PublicApiResource> collectionHolder;
    @Mock
    private TrackStorage trackStorage;

    @Before
    public void setup() throws IOException {
        initMocks(this);
        remoteCollectionLoader = new RemoteCollectionLoader<PublicApiResource>(trackStorage);

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
        List<PublicApiResource> collection = new ArrayList<PublicApiResource>();
        String nextHref = "linkylink";
        when(collectionHolder.getCollection()).thenReturn(collection);
        when(collectionHolder.getNextHref()).thenReturn(nextHref);
        when(collectionHolder.moreResourcesExist()).thenReturn(true);

        ReturnData<PublicApiResource> responseData = remoteCollectionLoader.load(publicCloudApi, parameters);

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
        ReturnData<PublicApiResource> responseData = remoteCollectionLoader.load(publicCloudApi, parameters);
        assertThat(responseData.success, is(false));
        assertThat(responseData.wasRefresh, is(true));
        assertThat(responseData.responseCode, is(Consts.NOT_SET));
    }

    @Test
    public void shouldStoreTracksFromCollection() throws CreateModelException {
        List<PublicApiResource> collection = new ArrayList<PublicApiResource>();
        final PublicApiTrack track1 = ModelFixtures.create(PublicApiTrack.class);
        final PublicApiTrack track2 = ModelFixtures.create(PublicApiTrack.class);

        collection.add(ModelFixtures.create(PublicApiUser.class));
        collection.add(track1);
        collection.add(ModelFixtures.create(PublicApiUser.class));
        collection.add(track2);

        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(trackStorage.storeCollectionAsync(anyCollection())).thenReturn(observable);

        when(collectionHolder.getCollection()).thenReturn(collection);
        remoteCollectionLoader.load(publicCloudApi, parameters);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(trackStorage).storeCollectionAsync((Collection<PublicApiTrack>) captor.capture());
        expect(captor.getValue()).toContainExactly(track1, track2);

        expect(observable.subscribedTo()).toBeTrue();
    }
}
