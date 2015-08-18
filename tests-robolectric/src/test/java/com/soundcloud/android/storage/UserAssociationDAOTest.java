package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.database.Cursor;

@RunWith(DefaultTestRunner.class)
public class UserAssociationDAOTest extends AbstractDAOTest<UserAssociationDAO> {

    private static final long TARGET_USER_ID = 1L;
    public static final String TOKEN = "12345";

    public UserAssociationDAOTest() {
        super(new UserAssociationDAO(Robolectric.application.getContentResolver()));
    }

    @Before
    public void before() {
        super.before();
        insertUser();
    }

    @Test
    public void shouldInsertFollowing() {
        PublicApiUser user = new PublicApiUser(1);
        UserAssociation ua = new UserAssociation(UserAssociation.Type.FOLLOWING, user);
        ua.owner = new PublicApiUser(AbstractDAOTest.OWNER_ID);
        getDAO().create(ua);

        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(Content.ME_FOLLOWINGS).toHaveColumnAt(0, TableColumns.UserAssociationView._ID, user.getId());
        expect(Content.ME_FOLLOWINGS).toHaveColumnAt(0, TableColumns.UserAssociationView.USER_ASSOCIATION_OWNER_ID, AbstractDAOTest.OWNER_ID);
        expect(Content.ME_FOLLOWINGS).toHaveColumnAt(0, TableColumns.UserAssociationView.USER_ASSOCIATION_TYPE, CollectionItemTypes.FOLLOWING);
        expect(Content.ME_FOLLOWINGS).toHaveColumnAt(0, TableColumns.UserAssociationView._TYPE, PublicApiUser.TYPE);
    }

    @Test
    public void shouldInsertFollowingWithAdditionTimestamp() {
        PublicApiUser user = new PublicApiUser(1);
        UserAssociation ua = new UserAssociation(UserAssociation.Type.FOLLOWING, user);
        ua.owner = new PublicApiUser(AbstractDAOTest.OWNER_ID);
        ua.markForAddition(TOKEN);
        getDAO().create(ua);

        Cursor cursor = Robolectric.application.getContentResolver().query(Content.ME_FOLLOWINGS.uri, null, null, null, null);
        expect(cursor).not.toBeNull();
        expect(cursor.moveToPosition(0)).toBeTrue();
        expect(cursor.getLong(cursor.getColumnIndex(TableColumns.UserAssociationView.USER_ASSOCIATION_ADDED_AT))).toBeGreaterThan(0L);
        expect(cursor.getString(cursor.getColumnIndex(TableColumns.UserAssociationView.USER_ASSOCIATION_TOKEN))).toEqual(TOKEN);
        cursor.close();
    }

    @Test
    public void shouldInsertFollowingWithRemovalTimestamp() {
        PublicApiUser user = new PublicApiUser(1);
        UserAssociation ua = new UserAssociation(UserAssociation.Type.FOLLOWING, user);
        ua.owner = new PublicApiUser(AbstractDAOTest.OWNER_ID);
        ua.markForRemoval();
        getDAO().create(ua);

        Cursor cursor = Robolectric.application.getContentResolver().query(Content.ME_FOLLOWINGS.uri, null, null, null, null);
        expect(cursor).not.toBeNull();
        expect(cursor.moveToPosition(0)).toBeTrue();
        expect(cursor.getLong(cursor.getColumnIndex(TableColumns.UserAssociationView.USER_ASSOCIATION_REMOVED_AT))).toBeGreaterThan(0L);
        cursor.close();
    }

    @Test
    public void shouldInsertFollower() {
        PublicApiUser user = new PublicApiUser(1);
        UserAssociation ua = new UserAssociation(UserAssociation.Type.FOLLOWER, user);
        ua.owner = new PublicApiUser(AbstractDAOTest.OWNER_ID);
        getDAO().create(ua);

        expect(Content.ME_FOLLOWERS).toHaveCount(1);
        expect(Content.ME_FOLLOWERS).toHaveColumnAt(0, TableColumns.UserAssociationView._ID, user.getId());
        expect(Content.ME_FOLLOWERS).toHaveColumnAt(0, TableColumns.UserAssociationView.USER_ASSOCIATION_OWNER_ID, AbstractDAOTest.OWNER_ID);
        expect(Content.ME_FOLLOWERS).toHaveColumnAt(0, TableColumns.UserAssociationView.USER_ASSOCIATION_TYPE, CollectionItemTypes.FOLLOWER);
        expect(Content.ME_FOLLOWERS).toHaveColumnAt(0, TableColumns.UserAssociationView._TYPE, PublicApiUser.TYPE);
    }

    @Test
    public void shouldRemoveFollowing() {
        UserAssociation following = TestHelper.insertAsUserAssociation(new PublicApiUser(TARGET_USER_ID), UserAssociation.Type.FOLLOWING);
        TestHelper.insertAsUserAssociation(new PublicApiUser(123L), SoundAssociation.Type.FOLLOWER);
        expect(Content.ME_FOLLOWERS).toHaveCount(1);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(Content.USERS).toHaveCount(2);

        expect(getDAO().delete(following)).toBeTrue();
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);
        expect(Content.ME_FOLLOWERS).toHaveCount(1);
    }

    @Test
    public void shouldRemoveFollower() {
        UserAssociation follower = TestHelper.insertAsUserAssociation(new PublicApiUser(TARGET_USER_ID), UserAssociation.Type.FOLLOWER);
        TestHelper.insertAsUserAssociation(new PublicApiUser(123L), SoundAssociation.Type.FOLLOWING);
        expect(Content.ME_FOLLOWERS).toHaveCount(1);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(Content.USERS).toHaveCount(2);

        expect(getDAO().delete(follower)).toBeTrue();
        expect(Content.ME_FOLLOWERS).toHaveCount(0);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
    }

    private void insertUser() {
        PublicApiUser user = new PublicApiUser(TARGET_USER_ID);
        TestHelper.insertWithDependencies(user);
        expect(Content.USERS).toHaveCount(1);
    }
}
