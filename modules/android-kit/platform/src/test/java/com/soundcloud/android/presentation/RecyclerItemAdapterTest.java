package com.soundcloud.android.presentation;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecyclerItemAdapterTest extends AndroidUnitTest {

    @Mock private View itemView;
    @Mock private CellRenderer<String> cellRenderer;
    @Mock private View.OnClickListener clickListener;
    private ViewGroup parentView;
    private TestViewHolder viewHolder;

    private RecyclerItemAdapter<String, TestViewHolder> adapter;

    @Before
    public void setUp() throws Exception {
        parentView = new FrameLayout(context());
        viewHolder = new TestViewHolder(itemView);
        adapter = buildAdapter(cellRenderer);
    }

    @Test
    public void shouldAddItems() {
        assertThat(adapter.getItemCount()).isEqualTo(0);
        adapter.addItem("item");
        assertThat(adapter.getItemCount()).isEqualTo(1);
    }

    @Test
    public void shouldPrependItems() {
        assertThat(adapter.getItemCount()).isEqualTo(0);
        adapter.addItem("item1");
        assertThat(adapter.getItemCount()).isEqualTo(1);
        adapter.prependItem("item0");
        assertThat(adapter.getItemCount()).isEqualTo(2);

        List<String> items = adapter.getItems();
        assertThat(items.get(0)).isEqualTo("item0");
        assertThat(items.get(1)).isEqualTo("item1");
    }

    @Test
    public void shouldAddItemsFromObservableSequence() {
        Observable.just(Arrays.asList("one", "two", "three")).subscribe(adapter);
        assertThat(adapter.getItemCount()).isEqualTo(3);
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
    public void shouldCreateItemViewWithPresenter() {
        when(cellRenderer.createItemView(parentView)).thenReturn(itemView);
        adapter.addItem("item");
        adapter.onCreateViewHolder(parentView, 0);
        verify(cellRenderer).createItemView(parentView);
    }

    @Test
    public void shouldCreateItemViewForTwoDifferentViewTypes() {
        CellRenderer rendererOne = mock(CellRenderer.class);
        CellRenderer rendererTwo = mock(CellRenderer.class);
        adapter = buildAdapter(new CellRendererBinding<>(0, rendererOne), new CellRendererBinding<>(1, rendererTwo));

        when(rendererOne.createItemView(parentView)).thenReturn(itemView);
        when(rendererTwo.createItemView(parentView)).thenReturn(itemView);

        adapter.onCreateViewHolder(parentView, 0);
        verify(rendererOne).createItemView(parentView);

        adapter.onCreateViewHolder(parentView, 1);
        verify(rendererTwo).createItemView(parentView);
    }

    @Test
    public void shouldBindItemView() {
        when(cellRenderer.createItemView(parentView)).thenReturn(itemView);
        adapter.addItem("item");

        adapter.onBindViewHolder(viewHolder, 0);
        verify(cellRenderer).bindItemView(0, itemView, Collections.singletonList("item"));
    }

    @Test
    public void shouldSetCustomClickListenerOnItemView() throws Exception {
        adapter.setOnItemClickListener(clickListener);
        when(cellRenderer.createItemView(parentView)).thenReturn(itemView);

        final TestViewHolder testViewHolder = adapter.onCreateViewHolder(parentView, 0);
        adapter.onBindViewHolder(testViewHolder, 1);

        verify(itemView).setOnClickListener(clickListener);
    }

    @Test
    public void shouldSetBackgroundResourceOnItemView() throws Exception {
        adapter.setOnItemClickListener(clickListener);
        when(cellRenderer.createItemView(parentView)).thenReturn(itemView);

        final TestViewHolder testViewHolder = adapter.onCreateViewHolder(parentView, 0);
        adapter.onBindViewHolder(testViewHolder, 1);

        verify(itemView).setBackgroundResource(anyInt());
    }

    @Test
    public void shouldSetPaddingOnItemView() throws Exception {
        when(itemView.getPaddingLeft()).thenReturn(1);
        when(itemView.getPaddingTop()).thenReturn(2);
        when(itemView.getPaddingRight()).thenReturn(3);
        when(itemView.getPaddingBottom()).thenReturn(4);

        adapter.setOnItemClickListener(clickListener);
        when(cellRenderer.createItemView(parentView)).thenReturn(itemView);

        final TestViewHolder testViewHolder = adapter.onCreateViewHolder(parentView, 0);
        adapter.onBindViewHolder(testViewHolder, 1);

        verify(itemView).setPadding(1, 2, 3, 4);
    }

    @Test
    public void shouldNotSetCustomClickListenerOnItemView() throws Exception {
        when(cellRenderer.createItemView(parentView)).thenReturn(itemView);

        final TestViewHolder testViewHolder = adapter.onCreateViewHolder(parentView, 0);
        adapter.onBindViewHolder(testViewHolder, 1);

        verify(itemView, never()).setOnClickListener(any(View.OnClickListener.class));
        verify(itemView, never()).setBackgroundResource(anyInt());
    }

    private RecyclerItemAdapter<String, TestViewHolder> buildAdapter(final CellRendererBinding... bindings) {
        return new RecyclerItemAdapter<String, TestViewHolder>(bindings) {
            @Override
            public int getBasicItemViewType(int position) {
                return 0;
            }

            @Override
            protected TestViewHolder createViewHolder(View itemView) {
                return new TestViewHolder(itemView);
            }
        };
    }

    private RecyclerItemAdapter<String, TestViewHolder> buildAdapter(CellRenderer<String> cellRenderer) {
        return new RecyclerItemAdapter<String, TestViewHolder>(cellRenderer) {
            @Override
            protected TestViewHolder createViewHolder(View itemView) {
                return new TestViewHolder(itemView);
            }

            @Override
            public int getBasicItemViewType(int position) {
                return 0;
            }
        };
    }

    private static class TestViewHolder extends RecyclerView.ViewHolder {

        public TestViewHolder(View itemView) {
            super(itemView);
        }
    }
}
