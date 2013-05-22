package com.soundcloud.android.dao;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.Date;
import java.util.List;

/**
 * Use this storage facade to persist information about user-to-user relations to the database.
 * These relations currently are: followers and followings.
 *
 * @see com.soundcloud.android.model.UserAssociation.Type
 */
public class UserAssociationStorage {
    private final ContentResolver mResolver;
    private final UserAssociationDAO mFollowingsDAO;


    public UserAssociationStorage() {
        mResolver = SoundCloudApplication.instance.getContentResolver();
        mFollowingsDAO = UserAssociationDAO.forContent(Content.ME_FOLLOWINGS, mResolver);
    }

    public List<Long> getStoredIds(Uri uri) {
        return ResolverHelper.idCursorToList(
            mResolver.query(
            uri.buildUpon().appendQueryParameter(ScContentProvider.Parameter.IDS_ONLY, "1").build(),
            null,
            null,
            null,
            null)
        );
    }

    /**
     * Persists user-followings information to the database. Will create a user association,
     * update the followers count of the target user, and commit to the database.
     * @param user the user that is being followed
     * @return the new association created
     */
    public UserAssociation addFollowing(User user) {
        UserAssociation.Type assocType = UserAssociation.Type.FOLLOWING;
        UserAssociation following = new UserAssociation(user, new Date(), assocType);
        user.addAFollower();
        mFollowingsDAO.create(following);
        return following;
    }

    /**
     * Remove a following for the logged in user. This will create an association, remove
     * it from the database, and update the corresponding user with the new count in local storage
     * @param user the user whose following should be removed
     * @return
     */
    public UserAssociation removeFollowing(User user) {
        UserAssociation.Type assocType = SoundAssociation.Type.FOLLOWING;
        UserAssociation following = new UserAssociation(user, new Date(), assocType);

        if (mFollowingsDAO.delete(following) && user.removeAFollower()) {
            new UserDAO(mResolver).update(user);
        }
        return following;
    }
}
