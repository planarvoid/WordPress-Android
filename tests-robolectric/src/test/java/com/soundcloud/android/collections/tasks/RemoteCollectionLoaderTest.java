package com.soundcloud.android.collections.tasks;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class RemoteCollectionLoaderTest {

    private RemoteCollectionLoader<PublicApiResource> remoteCollectionLoader;
    @Mock
    private PublicApi publicCloudApi;
    @Mock
    private CollectionParams<PublicApiResource> parameters;
    @Mock
    private Request request;
    @Mock
    private PublicApiResource.ResourceHolder<PublicApiResource> collectionHolder;

    @Before
    public void setup() throws IOException {
        initMocks(this);
        remoteCollectionLoader = new RemoteCollectionLoader<>();

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
        List<PublicApiResource> collection = new ArrayList<>();
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
}
