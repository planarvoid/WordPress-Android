package com.soundcloud.android.search;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.collections.views.UserlistRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import android.content.Context;
import android.view.ViewGroup;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultPresenterTest {

    @InjectMocks
    private SearchResultPresenter presenter;

    private Context context;

    @Mock
    private ImageOperations imageOperations;

    @Before
    public void setup() {
        context = Robolectric.application.getApplicationContext();
    }

    @Test
    public void shouldCreateUserItemView() throws Exception {
        ViewGroup parent = mock(ViewGroup.class);
        when(parent.getContext()).thenReturn(context);

        assertThat(presenter.createItemView(0, parent, SearchResultPresenter.TYPE_USER), instanceOf(UserlistRow.class));
    }

    @Test
    public void shouldCreatePlayableItemView() throws Exception {
        ViewGroup parent = mock(ViewGroup.class);
        when(parent.getContext()).thenReturn(context);

        assertThat(presenter.createItemView(0, parent, SearchResultPresenter.TYPE_PLAYABLE), instanceOf(PlayableRow.class));
    }

    @Test
    public void shouldBindItemView() throws Exception {
        UserlistRow view = mock(UserlistRow.class);
        ScResource user = new User();

        presenter.bindItemView(0, view, ItemAdapter.DEFAULT_ITEM_VIEW_TYPE, Arrays.asList(user));
        verify(view).display(0, user);
    }
}