package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ItemAdapterTest {

    @Mock private CellPresenter<String> cellPresenter;

    private ItemAdapter<String> adapter;

    @Before
    public void setup() {
        adapter = new ItemAdapter<>(cellPresenter);
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
    public void shouldGetItem() {
        adapter.addItem("item");
        expect(adapter.getItem(0)).toEqual("item");
    }

    @Test
    public void shouldGetItems() {
        adapter.addItem("item1");
        adapter.addItem("item2");

        List<String> items = adapter.getItems();

        expect(items.size()).toEqual(2);
        expect(items.get(0)).toEqual("item1");
        expect(items.get(1)).toEqual("item2");
    }

    @Test
    public void shouldPrependItem() {
        adapter.addItem("item1");

        adapter.prependItem("item0");

        List<String> items = adapter.getItems();
        expect(items.size()).toEqual(2);
        expect(items.get(0)).toEqual("item0");
        expect(items.get(1)).toEqual("item1");
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
    public void shouldDefaultToIdentityForItemIdFunction() {
        expect(adapter.getItemId(1)).toBe(1L);
    }

    @Test
    public void shouldCreateItemViewWithPresenter() {
        FrameLayout parent = mock(FrameLayout.class);
        adapter.addItem("item");
        adapter.getView(0, null, parent);
        verify(cellPresenter).createItemView(parent);
    }

    @Test
    public void shouldCreateItemViewForTwoDifferentViewTypes() {
        FrameLayout parent = mock(FrameLayout.class);
        CellPresenter presenterOne = mock(CellPresenter.class);
        CellPresenter presenterTwo = mock(CellPresenter.class);
        adapter = new ItemAdapter<String>(
                new CellPresenterBinding<String>(0, presenterOne),
                new CellPresenterBinding<String>(1, presenterTwo)) {
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
        when(cellPresenter.createItemView(parent)).thenReturn(itemView);
        adapter.addItem("item");

        adapter.getView(0, null, parent);
        verify(cellPresenter).bindItemView(0, itemView, Arrays.asList("item"));
    }

    @Test
    public void shouldConvertItemView() {
        FrameLayout parent = mock(FrameLayout.class);
        View convertView = mock(View.class);
        adapter.addItem("item");

        View itemView = adapter.getView(0, convertView, parent);
        expect(itemView).toBe(convertView);
        verify(cellPresenter, never()).createItemView(any(ViewGroup.class));
        verify(cellPresenter).bindItemView(0, itemView, Arrays.asList("item"));
    }

}
