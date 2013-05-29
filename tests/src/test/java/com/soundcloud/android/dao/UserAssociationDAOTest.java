package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.provider.ScContentProvider.CollectionItemTypes;

import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class UserAssociationDAOTest extends AbstractDAOTest<UserAssociationDAO> {

    private static final long OWNER_ID = 100L;
    private static final long TARGET_USER_ID = 1L;

    public UserAssociationDAOTest() {
        super(new UserAssociationDAO(Robolectric.application.getContentResolver()));
    }

    @Before
    public void before() {
        super.before();
        insertUser();
    }

    @Test
    public void shouldQueryForAll() {
        expect(getDAO().queryAll()).toNumber(0);
        TestHelper.insertAsUserAssociation(new User(TARGET_USER_ID), SoundAssociation.Type.FOLLOWING);
        TestHelper.insertAsUserAssociation(new User(TARGET_USER_ID), SoundAssociation.Type.FOLLOWER);
        expect(getDAO().queryAll()).toNumber(2);
    }

    @Test
    public void shouldInsertFollowing() {
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);

        User user = new User(1);
        UserAssociation ua = new UserAssociation(UserAssociation.Type.FOLLOWING, user);
        ua.owner = new User(AbstractDAOTest.OWNER_ID);
        getDAO().create(ua);

        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(Content.ME_FOLLOWINGS).toHaveColumnAt(0, DBHelper.UserAssociationView._ID, user.getId());
        expect(Content.ME_FOLLOWINGS).toHaveColumnAt(0, DBHelper.UserAssociationView.USER_ASSOCIATION_OWNER_ID, AbstractDAOTest.OWNER_ID);
        expect(Content.ME_FOLLOWINGS).toHaveColumnAt(0, DBHelper.UserAssociationView.USER_ASSOCIATION_TYPE, CollectionItemTypes.FOLLOWING);
        expect(Content.ME_FOLLOWINGS).toHaveColumnAt(0, DBHelper.UserAssociationView._TYPE, User.TYPE);
    }

    @Test
    public void shouldInsertFollower() {
        expect(Content.ME_FOLLOWERS).toHaveCount(0);

        User user = new User(1);
        UserAssociation ua = new UserAssociation(UserAssociation.Type.FOLLOWER, user);
        ua.owner = new User(AbstractDAOTest.OWNER_ID);
        getDAO().create(ua);

        expect(Content.ME_FOLLOWERS).toHaveCount(1);
        expect(Content.ME_FOLLOWERS).toHaveColumnAt(0, DBHelper.UserAssociationView._ID, user.getId());
        expect(Content.ME_FOLLOWERS).toHaveColumnAt(0, DBHelper.UserAssociationView.USER_ASSOCIATION_OWNER_ID, AbstractDAOTest.OWNER_ID);
        expect(Content.ME_FOLLOWERS).toHaveColumnAt(0, DBHelper.UserAssociationView.USER_ASSOCIATION_TYPE, CollectionItemTypes.FOLLOWER);
        expect(Content.ME_FOLLOWERS).toHaveColumnAt(0, DBHelper.UserAssociationView._TYPE, User.TYPE);
    }

    @Test
    public void shouldRemoveFollowing() {
        UserAssociation following = TestHelper.insertAsUserAssociation(new User(TARGET_USER_ID), UserAssociation.Type.FOLLOWING);
        TestHelper.insertAsUserAssociation(new User(123L), SoundAssociation.Type.FOLLOWER);
        expect(Content.ME_FOLLOWERS).toHaveCount(1);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(Content.USERS).toHaveCount(2);

        expect(getDAO().delete(following)).toBeTrue();
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);
        expect(Content.ME_FOLLOWERS).toHaveCount(1);
    }

    @Test
    public void shouldRemoveFollower() {
        UserAssociation follower = TestHelper.insertAsUserAssociation(new User(TARGET_USER_ID), UserAssociation.Type.FOLLOWER);
        TestHelper.insertAsUserAssociation(new User(123L), SoundAssociation.Type.FOLLOWING);
        expect(Content.ME_FOLLOWERS).toHaveCount(1);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(Content.USERS).toHaveCount(2);

        expect(getDAO().delete(follower)).toBeTrue();
        expect(Content.ME_FOLLOWERS).toHaveCount(0);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
    }

    private void insertUser() {
        User user = new User(TARGET_USER_ID);
        TestHelper.insertWithDependencies(user);
        expect(Content.USERS).toHaveCount(1);
    }
}
