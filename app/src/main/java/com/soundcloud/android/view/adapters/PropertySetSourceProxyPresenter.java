package com.soundcloud.android.view.adapters;

import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserProperty;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class PropertySetSourceProxyPresenter implements CellPresenter<ScResource> {
    private final CellPresenter<PropertySet> wrappedCellPresenter;
    private FollowingOperations followingOperations;

    public PropertySetSourceProxyPresenter(CellPresenter<PropertySet> cellPresenter,
                                           FollowingOperations followingOperations) {
        this.wrappedCellPresenter = cellPresenter;
        this.followingOperations = followingOperations;
    }

    @Override
    public View createItemView(int position, ViewGroup parent) {
        return wrappedCellPresenter.createItemView(position, parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ScResource> resources) {
        List<PropertySet> propertySets = new ArrayList<PropertySet>(resources.size());
        for (ScResource resource : resources) {
            if (resource instanceof PropertySetSource) {
                PropertySet propertySet = ((PropertySetSource) resource).toPropertySet();
                if (resource instanceof User) {
                    propertySet.put(UserProperty.IS_FOLLOWED_BY_ME, followingOperations.isFollowing(((User)resource).getUrn()));
                }
                propertySets.add(propertySet);
            } else {
                throw new IllegalArgumentException("Resource is not PropertySetSource : " + resource);
            }
        }
        wrappedCellPresenter.bindItemView(position, itemView, propertySets);
    }
}
