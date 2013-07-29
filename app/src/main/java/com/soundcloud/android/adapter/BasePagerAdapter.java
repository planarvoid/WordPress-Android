package com.soundcloud.android.adapter;


import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This is an implementation of {@link PagerAdapter} that wraps it up like the
 * {@link Adapter} interface.
 *
 * @author bowang
 *
 * http://calciumion.com/2013/01/17/viewpager-pageradapter-and-recycling/
 *
 */
public abstract class BasePagerAdapter<T> extends PagerAdapter {

    ArrayList<T> instantiatedItems = new ArrayList<T>();
    ArrayList<T> destroyedItems = new ArrayList<T>();

    @Override
    public final void startUpdate(ViewGroup container) {
        instantiatedItems.clear();
        destroyedItems.clear();
    }

    @Override
    public final T instantiateItem(ViewGroup container, int position) {
        final T o = getItem(position);
        instantiatedItems.add(o);
        return o;
    }

    @Override
    public final void destroyItem(ViewGroup container, int position, Object object) {
        destroyedItems.add((T) object);
    }

    @Override
    public final void finishUpdate(ViewGroup container) {
        ArrayList<View> recycledViews = new ArrayList<View>();

        // Remove views backing destroyed items from the specified container,
        // and queue them for recycling.
        for (int i = 0; destroyedItems.size() > 0 && i < container.getChildCount(); i++) {
            View v = container.getChildAt(i);
            Iterator<T> it = destroyedItems.iterator();
            while (it.hasNext()) {
                if (isViewFromObject(v, it.next())) {
                    container.removeView(v);
                    recycledViews.add(v);
                    it.remove();
                    break;
                }
            }
        }

        // Render views and attach them to the container. Page views are reused
        // whenever possible.
        for (T instantiatedItem : instantiatedItems) {
            View convertView = null;
            if (recycledViews.size() > 0){
                convertView = recycledViews.remove(0);
            }
            convertView = getView(instantiatedItem, convertView, container);
            convertView.setTag(instantiatedItem);
            container.addView(convertView);
        }

        instantiatedItems.clear();
        recycledViews.clear();
    }

    @Override
    public final boolean isViewFromObject(View view, Object object) {
        return view.getTag() != null && view.getTag() == object;
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position
     *            Position of the item whose data we want within the adapter's
     *            data set.
     * @return The data at the specified position
     */
    protected abstract T getItem(int position);

    /**
     * Get a View that displays the data at the specified position in the data
     * set.
     *
     * @param dataItem
     *            The data item whose view we want to render.
     * @param convertView
     *            The view to be reused.
     * @param parent
     *            The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    protected abstract View getView(T dataItem, View convertView, ViewGroup parent);

}
