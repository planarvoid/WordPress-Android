package com.soundcloud.android.model;

import java.util.ArrayList;

public class EmptyCollectionHolder extends CollectionHolder {
    public EmptyCollectionHolder(){
        this.collection = new ArrayList<ScResource>();
    }
}
