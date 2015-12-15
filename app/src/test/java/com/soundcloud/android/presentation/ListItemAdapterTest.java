package com.soundcloud.android.presentation;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Arrays;
import java.util.List;

public class ListItemAdapterTest extends AndroidUnitTest {

    @Mock private CellRenderer<String> cellRenderer;

    private ListItemAdapter<String> adapter;

    @Before
    public void setup() {
        adapter = new ListItemAdapter<>(cellRenderer);
    }

    @Test
    public void shouldAddItems() {
        assertThat(adapter.getItemCount()).isEqualTo(0);
        adapter.addItem("item");
        assertThat(adapter.getItemCount()).isEqualTo(1);
    }

    @Test
    public void shouldAddItemsFromObservableSequence() {
        Observable.just(Arrays.asList("one", "two", "three")).subscribe(adapter);
        assertThat(adapter.getItemCount()).isEqualTo(3);
    }

    @Test
    public void shouldGetItem() {
        adapter.addItem("item");
        assertThat(adapter.getItem(0)).isEqualTo("item");
    }

    @Test
    public void shouldGetItems() {
        adapter.addItem("item1");
        adapter.addItem("item2");

        List<String> items = adapter.getItems();

        assertThat(items.size()).isEqualTo(2);
        assertThat(items.get(0)).isEqualTo("item1");
        assertThat(items.get(1)).isEqualTo("item2");
    }

    @Test
    public void shouldPrependItem() {
        adapter.addItem("item1");

        adapter.prependItem("item0");

        List<String> items = adapter.getItems();
        assertThat(items.size()).isEqualTo(2);
        assertThat(items.get(0)).isEqualTo("item0");
        assertThat(items.get(1)).isEqualTo("item1");
    }

    @Test
    public void shouldRemoveItemAtPosition() {
        adapter.addItem("item1");
        adapter.addItem("item2");
        adapter.addItem("item3");

        adapter.removeItem(1);

        List<String> items = adapter.getItems();
        assertThat(items.size()).isEqualTo(2);
        assertThat(items.get(0)).isEqualTo("item1");
        assertThat(items.get(1)).isEqualTo("item3");
    }

    @Test
    public void shouldDefaultToIdentityForItemIdFunction() {
        assertThat(adapter.getItemId(1)).isEqualTo(1L);
    }

    @Test
    public void shouldCreateItemViewWithPresenter() {
        FrameLayout parent = mock(FrameLayout.class);
        adapter.addItem("item");
        adapter.getView(0, null, parent);
        verify(cellRenderer).createItemView(parent);
    }

    @Test
    public void shouldCreateItemViewForTwoDifferentViewTypes() {
        FrameLayout parent = mock(FrameLayout.class);
        CellRenderer<String> presenterOne = mock(CellRenderer.class);
        CellRenderer<String> presenterTwo = mock(CellRenderer.class);
        adapter = new ListItemAdapter<String>(
                new CellRendererBinding<>(0, presenterOne),
                new CellRendererBinding<>(1, presenterTwo)) {
            @Override
            public int getItemViewType(int position) {
                return position;
            }
        };

        adapter.getView(0, null, parent);
        verify(presenterOne).createItemView(parent);

        adapter.getView(1, null, parent);
        verify(presenterTwo).createItemView(parent);
    }

    @Test
    public void shouldBindItemView() {
        FrameLayout parent = mock(FrameLayout.class);
        View itemView = mock(View.class);
        when(cellRenderer.createItemView(parent)).thenReturn(itemView);
        adapter.addItem("item");

        adapter.getView(0, null, parent);
        verify(cellRenderer).bindItemView(0, itemView, singletonList("item"));
    }

    @Test
    public void shouldConvertItemView() {
        FrameLayout parent = mock(FrameLayout.class);
        View convertView = mock(View.class);
        adapter.addItem("item");

        View itemView = adapter.getView(0, convertView, parent);
        assertThat(itemView).isEqualTo(convertView);
        verify(cellRenderer, never()).createItemView(any(ViewGroup.class));
        verify(cellRenderer).bindItemView(0, itemView, singletonList("item"));
    }

}
