package com.soundcloud.android.discovery;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

public class SearchItemRendererTest extends AndroidUnitTest {

    private SearchItemRenderer renderer;

    @Mock private View itemView;
    @Mock private SearchItemRenderer.SearchListener listener;

    private EditText searchView;

    @Before
    public void setUp() {
        renderer = new SearchItemRenderer();
        searchView = new AutoCompleteTextView(context());

        when(itemView.findViewById(anyInt())).thenReturn(searchView);
    }

    @Test
    public void shouldCallSearchListenerOnClick() {
        renderer.setSearchListener(listener);
        renderer.onSearchClick(searchView);

        verify(listener).onSearchClicked(searchView.getContext());
    }
}
