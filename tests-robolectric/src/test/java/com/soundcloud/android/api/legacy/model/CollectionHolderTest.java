package com.soundcloud.android.api.legacy.model;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

public class CollectionHolderTest {
    private CollectionHolder<PublicApiResource> collectionHolder;
    @Mock
    private PublicApiResource scResource;
    @Mock
    private UnknownResource unknownResource;

    @Before
    public void setup(){
        initMocks(this);
        List<PublicApiResource> resources = Lists.newArrayList(scResource, unknownResource);
        collectionHolder = new CollectionHolder<>(resources);
    }

    @Test
    public void shouldFilterUnknownResourcesFromCollection(){
        assertThat(collectionHolder.size(), is(2));
        assertThat(collectionHolder.getCollection().contains(unknownResource), is(true));
        collectionHolder.removeUnknownResources();
        assertThat(collectionHolder.size(), is(1));
        assertThat(collectionHolder.get(0), is(scResource));
    }


}
