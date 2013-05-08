package com.soundcloud.android.task.collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.api.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class RemoteCollectionLoaderTest {

    private RemoteCollectionLoader<ScResource> remoteCollectionLoader;
    @Mock
    private AndroidCloudAPI androidCloudApi;
    @Mock
    private CollectionParams<ScResource> parameters;
    @Mock
    private Request request;
    @Mock
    private CollectionHolder<ScResource> collectionHolder;

    @Before
    public void setup() throws IOException {
        initMocks(this);
        remoteCollectionLoader = new RemoteCollectionLoader<ScResource>();

        stub(parameters.getRequest()).toReturn(request);
        stub(androidCloudApi.readCollection(request)).toReturn(collectionHolder);
    }


    @Test
    public void shouldRemoveUnknownResourcesFromCollectionHolder() throws IOException {
        remoteCollectionLoader.load(androidCloudApi,parameters);
        verify(collectionHolder).removeUnknownResources();
    }

    @Test
    public void shouldReturnAResultWithInformationFromTheCollectionHolder(){
        List<ScResource> collection = new ArrayList<ScResource>();
        String nextHref = "linkylink";
        when(collectionHolder.getCollection()).thenReturn(collection);
        when(collectionHolder.getNextHref()).thenReturn(nextHref);
        when(collectionHolder.moreResourcesExist()).thenReturn(true);

        ReturnData<ScResource> responseData = remoteCollectionLoader.load(androidCloudApi, parameters);

        assertThat(responseData.newItems, is(collection));
        assertThat(responseData.success, is(true));
        assertThat(responseData.keepGoing, is(true));
        assertThat(responseData.nextHref, is(nextHref));
        assertThat(responseData.responseCode, is(200));
    }

    @Test
    public void shouldReturnAResultThatSpecificAnErrorIfIOExceptionOccurs() throws IOException {
        when(parameters.isRefresh()).thenReturn(true);
        when(androidCloudApi.readCollection(request)).thenThrow(IOException.class);
        ReturnData<ScResource> responseData = remoteCollectionLoader.load(androidCloudApi, parameters);
        assertThat(responseData.success, is(false));
        assertThat(responseData.wasRefresh, is(true));
        assertThat(responseData.responseCode, is(EmptyListView.Status.CONNECTION_ERROR));

    }
}
