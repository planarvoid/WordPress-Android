package com.soundcloud.android.collections.tasks;


import com.soundcloud.android.api.legacy.model.ScModel;
import com.soundcloud.android.api.legacy.model.activities.Activity;

class CollectionLoaderFactory {

    public CollectionLoader createCollectionLoader(CollectionParams parameters){

        if (resourceIsASoundcloudActivity(parameters)) {
            return new ActivitiesLoader();
        }

        if (collectionIsLocatedRemotely(parameters)) {
            return new RemoteCollectionLoader();
        }

        return null;

    }

    private boolean resourceIsASoundcloudActivity(CollectionParams params) {
        Class<? extends ScModel> resourceType = params.getContent().getModelType();
        return resourceType != null && Activity.class.isAssignableFrom(resourceType);
    }

    private boolean collectionIsLocatedRemotely(CollectionParams params) {
        return params.getRequest() != null;
    }
}
