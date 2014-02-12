package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.collections.views.UserlistRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.view.ViewGroup;


@RunWith(SoundCloudTestRunner.class)
public class SearchResultsAdapterTest {

    private SearchResultsAdapter adapter;

    private Context context;

    @Mock
    ImageOperations imageOperations;


    @Before
    public void setUp() throws Exception {
        context = Robolectric.application.getApplicationContext();
        adapter = new SearchResultsAdapter(imageOperations);
    }

    @Test
    public void shouldAddItemsToAdapterOnNext() {
        ScResource resource1 = mock(ScResource.class);
        ScResource resource2 = mock(ScResource.class);
        SearchResultsCollection results = new SearchResultsCollection();
        results.setCollection(Lists.newArrayList(resource1, resource2));

        adapter.onNext(results);

        assertEquals(adapter.getItem(0), resource1);
        assertEquals(adapter.getItem(1), resource2);
    }

    @Test
    public void shouldCreateUserItemView() throws Exception {
        User user = mock(User.class);
        adapter.addItem(user);

        ViewGroup parent = mock(ViewGroup.class);
        when(parent.getContext()).thenReturn(context);

        assertThat(adapter.createItemView(0, parent), instanceOf(UserlistRow.class));
    }

    @Test
    public void shouldDifferentiateItemViewTypes() {
        User user = mock(User.class);
        Track track = mock(Track.class);
        adapter.addItem(user);
        adapter.addItem(track);

        expect(adapter.getItemViewType(0)).toEqual(1);
        expect(adapter.getItemViewType(1)).toEqual(0);
    }

    @Test
    public void shouldBindItemView() throws Exception {
        UserlistRow view = mock(UserlistRow.class);
        User user = mock(User.class);
        adapter.addItem(user);

        adapter.bindItemView(0, view);
        verify(view).display(0, user);
    }

}
