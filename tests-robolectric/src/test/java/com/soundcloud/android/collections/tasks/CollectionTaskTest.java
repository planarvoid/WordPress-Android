package com.soundcloud.android.collections.tasks;


import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class CollectionTaskTest {
    private CollectionTask collectionTask;
    @Mock
    private PublicApiWrapper publicCloudApi;
    @Mock
    private CollectionTask.Callback callback;
    @Mock
    private CollectionLoader loader;
    @Mock
    private CollectionLoaderFactory collectionLoaderFactory;
    @Mock
    private CollectionParams collectionParams;
    @Mock
    private ReturnData validReturnData;

    @Before
    public void setUp(){
        initMocks(this);
        collectionTask = new CollectionTask(publicCloudApi, callback, collectionLoaderFactory);
    }

    @Test
    public void shouldLoadCollectionFromLoaderReturnedFromFactory(){
        when(collectionLoaderFactory.createCollectionLoader(collectionParams)).thenReturn(loader);
        collectionTask.doInBackground(collectionParams);
        verify(loader).load(publicCloudApi,  collectionParams);
    }

    @Test
    public void shouldReturnDataFromLoader(){
        when(collectionLoaderFactory.createCollectionLoader(collectionParams)).thenReturn(loader);
        when(loader.load(publicCloudApi, collectionParams)).thenReturn(validReturnData);
        assertThat(collectionTask.doInBackground(collectionParams), is(validReturnData));
    }

    @Test
    public void shouldReturnAnEmptyDataSetIfContentDoesNotMatchAnyLoader(){
        when(collectionLoaderFactory.createCollectionLoader(collectionParams)).thenReturn(null);
        ReturnData returnData = collectionTask.doInBackground(collectionParams);
        assertThat(returnData, is(not(validReturnData)));
        assertThat(returnData.success, is(false));
        assertThat(returnData.newItems, is(nullValue()));
    }

    @Test
    public void shouldCallCallbackWithResultIfCallbackIsNotNull(){
        collectionTask.onPostExecute(validReturnData);
        verify(callback).onPostTaskExecute(validReturnData);
    }

    @Test
    public void shouldHandleNonExistentCallbackReferenceGracefully(){
        collectionTask  = new CollectionTask(publicCloudApi, null, collectionLoaderFactory);
        collectionTask.onPostExecute(validReturnData);
    }


}
