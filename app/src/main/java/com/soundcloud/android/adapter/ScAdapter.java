package com.soundcloud.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class ScAdapter<ModelType, ViewType extends View> extends BaseAdapter implements ItemAdapter<ModelType> {

    protected final List<ModelType> mItems;

    protected ScAdapter(int initalDataSize) {
        mItems = new ArrayList<ModelType>(initalDataSize);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public ModelType getItem(int location) {
        return mItems.get(location);
    }

    public void addItem(ModelType item) {
        mItems.add(item);
    }

    public void clear() {
        mItems.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = createItemView(position, parent);
        }
        bindItemView(position, (ViewType) convertView);
        return convertView;
    }

    protected abstract ViewType createItemView(int position, ViewGroup parent);
    protected abstract void bindItemView(int position, ViewType itemView);

}
