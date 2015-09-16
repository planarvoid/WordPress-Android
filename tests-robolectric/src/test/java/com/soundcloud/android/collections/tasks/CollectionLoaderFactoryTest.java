package com.soundcloud.android.collections.tasks;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.api.legacy.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
@RunWith(SoundCloudTestRunner.class)
public class CollectionLoaderFactoryTest {
    private CollectionLoaderFactory collectionLoaderFactory;
    @Mock
    private CollectionParams collectionParams;

    @Before
    public void setUp(){
        initMocks(this);
        collectionLoaderFactory = new CollectionLoaderFactory();
    }

    @Test
    public void shouldCreateActivityLoaderIfResourceIsASoundCloudActivity(){
        when(collectionParams.getContent()).thenReturn(Content.ME_SOUND_STREAM);
        CollectionLoader loader = collectionLoaderFactory.createCollectionLoader(collectionParams);
        assertThat(loader instanceof ActivitiesLoader, is(true));
    }

    @Test
    public void shouldCreateRemoteLoaderIfParamsHasAnEmbeddedRequest(){
        when(collectionParams.getContent()).thenReturn(Content.COMMENTS);
        when(collectionParams.getRequest()).thenReturn(mock(Request.class));
        CollectionLoader loader = collectionLoaderFactory.createCollectionLoader(collectionParams);
        assertThat(loader instanceof RemoteCollectionLoader, is(true));
    }

    @Test
    public void shouldReturnNullIfContentDoesNotMatchAnyLoader(){
        when(collectionParams.getContent()).thenReturn(Content.UNKNOWN);
       assertThat(collectionLoaderFactory.createCollectionLoader(collectionParams), is(nullValue()));
    }
}
