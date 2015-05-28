package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class RecyclerViewAdapterTest {

    @Mock private View itemView;
    @Mock private ViewGroup parent;
    @Mock private CellRenderer<String> cellRenderer;
    @Mock private View.OnClickListener clickListener;

    private RecyclerViewAdapter<String, TestViewHolder> adapter;
    private TestViewHolder viewHolder;

    @Before
    public void setUp() throws Exception {
        adapter = buildAdapter(cellRenderer);
        viewHolder = new TestViewHolder(itemView);
        when(parent.getContext()).thenReturn(Robolectric.application);
    }

    @Test
    public void shouldAddItems() {
        expect(adapter.getItemCount()).toBe(0);
        adapter.addItem("item");
        expect(adapter.getItemCount()).toBe(1);
    }

    @Test
    public void shouldAddItemsFromObservableSequence() {
        Observable.just(Arrays.asList("one", "two", "three")).subscribe(adapter);
        expect(adapter.getItemCount()).toBe(3);
    }

    @Test
    public void shouldRemoveItemAtPosition() {
        adapter.addItem("item1");
        adapter.addItem("item2");
        adapter.addItem("item3");

        adapter.removeItem(1);

        List<String> items = adapter.getItems();
        expect(items.size()).toEqual(2);
        expect(items.get(0)).toEqual("item1");
        expect(items.get(1)).toEqual("item3");
    }

    @Test
    public void shouldCreateItemViewWithPresenter() {
        when(cellRenderer.createItemView(parent)).thenReturn(itemView);
        adapter.addItem("item");
        adapter.onCreateViewHolder(parent, 0);
        verify(cellRenderer).createItemView(parent);
    }

    @Test
    public void shouldCreateItemViewForTwoDifferentViewTypes() {
        CellRenderer rendererOne = mock(CellRenderer.class);
        CellRenderer rendererTwo = mock(CellRenderer.class);
        adapter = buildAdapter(new CellRendererBinding<>(0, rendererOne), new CellRendererBinding<>(1, rendererTwo));

        when(rendererOne.createItemView(parent)).thenReturn(itemView);
        when(rendererTwo.createItemView(parent)).thenReturn(itemView);

        adapter.onCreateViewHolder(parent, 0);
        verify(rendererOne).createItemView(parent);

        adapter.onCreateViewHolder(parent, 1);
        verify(rendererTwo).createItemView(parent);
    }

    @Test
    public void shouldBindItemView() {
        when(cellRenderer.createItemView(parent)).thenReturn(itemView);
        adapter.addItem("item");

        adapter.onBindViewHolder(viewHolder, 0);
        verify(cellRenderer).bindItemView(0, itemView, Arrays.asList("item"));
    }

    @Test
    public void shouldSetCustomClickListenerOnItemView() throws Exception {
        adapter.setOnItemClickListener(clickListener);
        when(cellRenderer.createItemView(parent)).thenReturn(itemView);

        final TestViewHolder testViewHolder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(testViewHolder, 1);

        verify(itemView).setOnClickListener(clickListener);
    }

    private RecyclerViewAdapter<String, TestViewHolder> buildAdapter(final CellRendererBinding... bindings) {
        return new RecyclerViewAdapter<String, TestViewHolder>(bindings) {
            @Override
            public int getItemViewType(int position) {
                return position;
            }

            @Override
            protected TestViewHolder createViewHolder(View itemView) {
                return new TestViewHolder(itemView);
            }
        };
    }

    private RecyclerViewAdapter<String, TestViewHolder> buildAdapter(CellRenderer<String> cellRenderer) {
        return new RecyclerViewAdapter<String, TestViewHolder>(cellRenderer) {
            @Override
            protected TestViewHolder createViewHolder(View itemView) {
                return new TestViewHolder(itemView);
            }
        };
    }

    private static class TestViewHolder extends RecyclerView.ViewHolder {

        public TestViewHolder(View itemView) {
            super(itemView);
        }
    }
}