package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.views.UserlistRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultsAdapterTest {

    SearchResultsAdapter adapter;

    Context context;

    @Mock
    ImageOperations imageOperations;

    @Mock
    PlaybackOperations playbackOperations;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.application.getApplicationContext();
        adapter = new SearchResultsAdapter(imageOperations, playbackOperations);
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

    @Test
    public void shouldHandlePlayableItemClick() throws Exception {
        Track track = mock(Track.class);
        adapter.addItem(track);

        adapter.handleClick(context, 0);

        verify(playbackOperations).playFromAdapter(eq(context), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_EVERYTHING));
    }

    @Test
    public void shouldHandleUserItemClick() throws Exception {
        User user = mock(User.class);
        adapter.addItem(user);

        Context mockContext = mock(Context.class);
        adapter.handleClick(mockContext, 0);

        verify(mockContext).startActivity(any(Intent.class));
    }
}
