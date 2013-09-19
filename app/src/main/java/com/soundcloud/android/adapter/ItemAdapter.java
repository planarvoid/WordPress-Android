package com.soundcloud.android.adapter;

public interface ItemAdapter<ModelType> {

    void addItem(ModelType item);
    void clear();
    void notifyDataSetChanged();

}
