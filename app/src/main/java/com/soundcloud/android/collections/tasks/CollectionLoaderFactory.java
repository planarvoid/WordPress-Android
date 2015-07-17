package com.soundcloud.android.collections.tasks;


import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.api.legacy.model.ScModel;

class CollectionLoaderFactory {

    public CollectionLoader createCollectionLoader(CollectionParams parameters){

        if (resourceIsASoundcloudActivity(parameters)) {
            return new ActivitiesLoader();
        }

        if (contentIsSyncable(parameters)) {
            return new MyCollectionLoader();
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

    private boolean contentIsSyncable(CollectionParams params) {
        return params.getContent().isSyncable();
    }

}
