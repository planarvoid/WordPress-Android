package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ItemAdapterTest {

    @Mock
    private CellPresenter cellPresenter;

    private ItemAdapter<PublicApiTrack> adapter;

    @Before
    public void setup() {
        adapter = new ItemAdapter<PublicApiTrack>(cellPresenter) {};
    }

    @Test
    public void shouldAddItems() {
        expect(adapter.getCount()).toBe(0);
        adapter.addItem(new PublicApiTrack());
        expect(adapter.getCount()).toBe(1);
    }

    @Test
    public void shouldGetItem() {
        PublicApiTrack item = new PublicApiTrack();
        adapter.addItem(item);
        expect(adapter.getItem(0)).toBe(item);
    }

    @Test
    public void shouldGetItems() {
        adapter.addItem(new PublicApiTrack(1));
        adapter.addItem(new PublicApiTrack(2));

        List<PublicApiTrack> items = adapter.getItems();

        expect(items.size()).toEqual(2);
        expect(items.get(0)).toEqual(new PublicApiTrack(1));
        expect(items.get(1)).toEqual(new PublicApiTrack(2));
    }

    @Test
    public void shouldDefaultToIdentityForItemIdFunction() {
        expect(adapter.getItemId(1)).toBe(1L);
    }

    @Test
    public void shouldCreateItemViewWithPresenter() {
        FrameLayout parent = mock(FrameLayout.class);
        adapter.addItem(new PublicApiTrack());
        adapter.getView(0, null, parent);
        verify(cellPresenter).createItemView(0, parent);
    }

    @Test
    public void shouldCreateItemViewForTwoDifferentViewTypes() {
        FrameLayout parent = mock(FrameLayout.class);
        CellPresenter presenterOne = mock(CellPresenter.class);
        CellPresenter presenterTwo = mock(CellPresenter.class);
        adapter = new ItemAdapter<PublicApiTrack>(
                new ItemAdapter.CellPresenterEntity(0, presenterOne),
                new ItemAdapter.CellPresenterEntity(1, presenterTwo)) {
            @Override
            public int getItemViewType(int position) {
                return position;
            }
        };

        adapter.getView(0, null, parent);
        verify(presenterOne).createItemView(0, parent);

        adapter.getView(1, null, parent);
        verify(presenterTwo).createItemView(1, parent);
    }

    @Test
    public void shouldBindItemView() {
        FrameLayout parent = mock(FrameLayout.class);
        View itemView = mock(View.class);
        when(cellPresenter.createItemView(0, parent)).thenReturn(itemView);
        PublicApiTrack item = new PublicApiTrack();
        adapter.addItem(item);

        adapter.getView(0, null, parent);
        verify(cellPresenter).bindItemView(0, itemView, Arrays.asList(item));
    }

    @Test
    public void shouldConvertItemView() {
        FrameLayout parent = mock(FrameLayout.class);
        View convertView = mock(View.class);
        PublicApiTrack item = new PublicApiTrack();
        adapter.addItem(item);

        View itemView = adapter.getView(0, convertView, parent);
        expect(itemView).toBe(convertView);
        verify(cellPresenter, never()).createItemView(anyInt(), any(ViewGroup.class));
        verify(cellPresenter).bindItemView(0, itemView, Arrays.asList(item));
    }

    @Test
    public void shouldSaveAllItemsInSaveInstanceState() {
        Bundle bundle = new Bundle();

        adapter.saveInstanceState(bundle);
        expect(bundle.containsKey(ItemAdapter.EXTRA_KEY_ITEMS)).toBeTrue();

        ArrayList<Parcelable> savedItems = bundle.getParcelableArrayList(ItemAdapter.EXTRA_KEY_ITEMS);
        expect(savedItems.size()).toEqual(adapter.getCount());
        for (int i = 0; i < adapter.getCount(); i++) {
            expect(savedItems.get(i)).toEqual(adapter.getItem(i));
        }
    }

    @Test
    public void shouldRestoreAllItemsInRestoreInstanceState() {
        expect(adapter.getCount()).toBe(0);

        Bundle bundle = new Bundle();
        ArrayList<PublicApiTrack> tracks = Lists.newArrayList(new PublicApiTrack(1), new PublicApiTrack(2));
        bundle.putParcelableArrayList(ItemAdapter.EXTRA_KEY_ITEMS, tracks);

        adapter.restoreInstanceState(bundle);
        expect(adapter.getCount()).toBe(2);

        for (int i = 0; i < adapter.getCount(); i++) {
            expect(adapter.getItem(i)).toEqual(tracks.get(i));
        }
    }

}
