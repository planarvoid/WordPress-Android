package com.soundcloud.android.task.collection;


import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.api.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SoundCloudTestRunner.class)
public class CollectionTaskTest {
    private CollectionTask collectionTask;
    @Mock
    private AndroidCloudAPI androidCloudApi;
    @Mock
    private CollectionTask.Callback callback;
    @Mock
    private CollectionLoader syncableCollectionLoader;
    @Mock
    private CollectionLoader remoteCollectionLoader;
    @Mock
    private CollectionLoader soundcloudActivityLoader;
    @Mock
    private CollectionParams collectionParams;
    @Mock
    private Request request;
    @Mock
    private ReturnData validReturnData;

    @Before
    public void setUp(){
        initMocks(this);
        collectionTask = new CollectionTask(androidCloudApi, callback, syncableCollectionLoader, remoteCollectionLoader, soundcloudActivityLoader);
    }

    @Test
    public void shouldLoadCollectionFromActivityLoaderIfResourceIsASoundCloudActivity(){
        when(collectionParams.getContent()).thenReturn(Content.ME_SOUND_STREAM);
        when(soundcloudActivityLoader.load(androidCloudApi, collectionParams)).thenReturn(validReturnData);
        ReturnData returnData = collectionTask.doInBackground(collectionParams);
        assertThat(returnData, is(validReturnData));
        verify(soundcloudActivityLoader).load(androidCloudApi,  collectionParams);
        verifyZeroInteractions(remoteCollectionLoader, syncableCollectionLoader);
    }

    @Test
    public void shouldLoadCollectionFromRemoteLoaderIfParamsHasAnEmbeddedRequest(){
        when(collectionParams.getContent()).thenReturn(Content.COMMENTS);
        when(collectionParams.getRequest()).thenReturn(request);
        when(remoteCollectionLoader.load(androidCloudApi, collectionParams)).thenReturn(validReturnData);
        ReturnData returnData = collectionTask.doInBackground(collectionParams);
        assertThat(returnData, is(validReturnData));
        verify(remoteCollectionLoader).load(androidCloudApi,  collectionParams);
        verifyZeroInteractions(soundcloudActivityLoader, syncableCollectionLoader);
    }

    @Test
    public void shouldLoadCollectionFromSyncableLoaderIfContentIsSyncable(){
        when(collectionParams.getContent()).thenReturn(Content.ME_PLAYLISTS);
        when(syncableCollectionLoader.load(androidCloudApi, collectionParams)).thenReturn(validReturnData);
        ReturnData returnData = collectionTask.doInBackground(collectionParams);
        assertThat(returnData, is(validReturnData));
        verify(syncableCollectionLoader).load(androidCloudApi,  collectionParams);
        verifyZeroInteractions(soundcloudActivityLoader, remoteCollectionLoader);
    }

    @Test
    public void shouldReturnAnEmptyDataSetIfContentDoesNotMatchAnyLoader(){
        when(collectionParams.getContent()).thenReturn(Content.UNKNOWN);
        ReturnData returnData = collectionTask.doInBackground(collectionParams);
        assertThat(returnData, is(not(validReturnData)));
        assertThat(returnData.success, is(false));
        assertThat(returnData.newItems, is(nullValue()));
        verifyZeroInteractions(soundcloudActivityLoader, remoteCollectionLoader, syncableCollectionLoader);
    }

    @Test
    public void shouldCallCallbackWithResultIfCallbackIsNotNull(){
        collectionTask.onPostExecute(validReturnData);
        verify(callback).onPostTaskExecute(validReturnData);
    }

    @Test
    public void shouldHandleNonExistenCallbackReferenceGraefully(){
        collectionTask  = new CollectionTask(androidCloudApi, null, syncableCollectionLoader, remoteCollectionLoader, soundcloudActivityLoader);
        collectionTask.onPostExecute(validReturnData);
    }


}
