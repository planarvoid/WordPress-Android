package com.soundcloud.android.view.adapters;

import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class PropertySetSourceProxyPresenter implements CellPresenter<PublicApiResource> {
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
    public void bindItemView(int position, View itemView, List<PublicApiResource> resources) {
        List<PropertySet> propertySets = new ArrayList<PropertySet>(resources.size());
        for (PublicApiResource resource : resources) {
            if (resource instanceof PropertySetSource) {
                PropertySet propertySet = ((PropertySetSource) resource).toPropertySet();
                if (resource instanceof PublicApiUser) {
                    propertySet.put(UserProperty.IS_FOLLOWED_BY_ME, followingOperations.isFollowing(((PublicApiUser)resource).getUrn()));
                }
                propertySets.add(propertySet);
            } else {
                throw new IllegalArgumentException("Resource is not PropertySetSource : " + resource);
            }
        }
        wrappedCellPresenter.bindItemView(position, itemView, propertySets);
    }
}
