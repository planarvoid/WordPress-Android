package com.soundcloud.android.view.adapters;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.api.legacy.model.UserHolder;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Temporarily used to adapt ScListFragment lists that use public API models to PropertySets and the new cell design
 */
public class UserAdapter extends ScBaseAdapter<PublicApiResource> implements FollowingOperations.FollowStatusChangedListener {

    @Inject UserItemPresenter presenter;
    @Inject FollowingOperations followingOperations;

    private final List<UserItem> users = new ArrayList<>(Consts.LIST_PAGE_SIZE);

    public UserAdapter(Uri uri) {
        super(uri);
        SoundCloudApplication.getObjectGraph().inject(this);
        init();
    }

    @VisibleForTesting
    UserAdapter(Uri uri, UserItemPresenter userItemPresenter, FollowingOperations followingOperations) {
        super(uri);
        this.presenter = userItemPresenter;
        this.followingOperations = followingOperations;
        init();
    }

    private void init() {
        followingOperations.requestUserFollowings(this);
    }

    @Override
    protected View createRow(Context context, int position, ViewGroup parent) {
        return presenter.createItemView(parent);
    }

    @Override
    protected void bindRow(int position, View rowView) {
        presenter.bindItemView(position, rowView, users);
    }

    @Override
    public void addItems(List<PublicApiResource> newItems) {
        final List<PublicApiResource> filteredItems = filterInvalidUsers(newItems);
        super.addItems(filteredItems);
        this.users.addAll(toPropertySets(filteredItems));
    }

    // Note: We filter out the users without a username, since these may not have been successfully synced;
    // this would later translate into an error on the UserItemPresenter, that requires the username property
    private List<PublicApiResource> filterInvalidUsers(List<PublicApiResource> newItems) {
        return Lists.newArrayList(Iterables.filter(newItems, new Predicate<PublicApiResource>() {
            @Override
            public boolean apply(PublicApiResource input) {
                PublicApiUser user = ((UserHolder) input).getUser();
                return ScTextUtils.isNotBlank(user.getUsername());
            }
        }));
    }

    @Override
    public void clearData() {
        super.clearData();
        users.clear();
    }

    public void updateItems(Map<Urn, PublicApiResource> updatedItems) {
        for (int i = 0; i < users.size(); i++) {
            final Urn key = users.get(i).getEntityUrn();
            if (updatedItems.containsKey(key)) {
                final PublicApiUser publicApiUser = (PublicApiUser) updatedItems.get(key);
                users.set(i, putFollowStatus(UserItem.from(publicApiUser.toPropertySet())));
            }
        }
        notifyDataSetChanged();
    }

    private List<UserItem> toPropertySets(List<PublicApiResource> items) {
        List<UserItem> newItems = new ArrayList<>(items.size());
        for (PublicApiResource item : items) {
            newItems.add(putFollowStatus(UserItem.from(getUser(item).toPropertySet())));
        }
        return newItems;
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        context.startActivity(
                new Intent(context, ProfileActivity.class)
                        .putExtra(ProfileActivity.EXTRA_USER, getUser(getItem(position)))
                        .putExtra(ProfileActivity.EXTRA_QUERY_SOURCE_INFO, searchQuerySourceInfo));
        return ItemClickResults.LEAVING;
    }

    private PublicApiUser getUser(PublicApiResource item) {
        return item instanceof UserAssociation ? ((UserAssociation) item).getUser() : (PublicApiUser) item;
    }

    @Override
    public void onFollowChanged() {
        for (UserItem propertySet : users){
            putFollowStatus(propertySet);
        }
        notifyDataSetChanged();
    }

    private UserItem putFollowStatus(UserItem source) {
        final boolean following = followingOperations.isFollowing(source.getEntityUrn());
        source.update(PropertySet.from(UserProperty.IS_FOLLOWED_BY_ME.bind(following)));
        return source;
    }
}
