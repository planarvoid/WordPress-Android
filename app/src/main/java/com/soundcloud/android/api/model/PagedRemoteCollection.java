package com.soundcloud.android.api.model;

import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.collections.PropertySet;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;

public class PagedRemoteCollection extends PagedCollection<PropertySet> {
    public static final Func1<ModelCollection<? extends PropertySetSource>, PagedRemoteCollection> TO_PAGED_REMOTE_COLLECTION =
            new Func1<ModelCollection<? extends PropertySetSource>, PagedRemoteCollection>() {
                @Override
                public PagedRemoteCollection call(ModelCollection<? extends PropertySetSource> modelCollection) {
                    return new PagedRemoteCollection(modelCollection);
                }
            };
    private static final Func1<List<? extends PropertySetSource>, List<PropertySet>> TO_PROPERTY_SETS =
            new Func1<List<? extends PropertySetSource>, List<PropertySet>>() {
        public List<PropertySet> call(List<? extends PropertySetSource> input) {
            ArrayList<PropertySet> items = new ArrayList<>(input.size());
            for (PropertySetSource source : input) {
                items.add(source.toPropertySet());
            }
            return items;
        }
    };

    public PagedRemoteCollection(ModelCollection<? extends PropertySetSource> propertySetSources) {
        super(new ModelCollection<>(TO_PROPERTY_SETS.call(propertySetSources.getCollection()),
                                    propertySetSources.getLinks()));
    }

    public PagedRemoteCollection(List<? extends PropertySetSource> items, String nextPage) {
        this(new ModelCollection<>(items, nextPage));
    }

}
