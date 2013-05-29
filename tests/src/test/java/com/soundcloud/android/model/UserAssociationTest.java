package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class UserAssociationTest {

    @Test
    public void shouldParcelAndUnparcelCorrectly() throws Exception {
        User u = new User(123L);
        UserAssociation userAssociation = new UserAssociation(Association.Type.FOLLOWING, u);

        expect(userAssociation.associationType).not.toBeNull();
        expect(userAssociation.created_at).not.toBeNull();
        expect(userAssociation.getUser()).not.toBeNull();

        Parcel p = Parcel.obtain();
        userAssociation.writeToParcel(p, 0);
        compareUserAssociations(new UserAssociation(p), userAssociation);
    }

    @Test
    public void shouldProvideUniqueListItemId() throws Exception {
        User u = new User(123L);
        UserAssociation UserAssociation1 = new UserAssociation(Association.Type.FOLLOWING, u);
        UserAssociation UserAssociation2 = new UserAssociation(Association.Type.FOLLOWER, u);
        expect(UserAssociation1.getListItemId()).not.toEqual(UserAssociation2.getListItemId());
    }

    @Test
    public void testEquals() {
        UserAssociation a1 = new UserAssociation(UserAssociation.Type.FOLLOWING, new User(1L));
        UserAssociation a2;

        a2 = new UserAssociation(UserAssociation.Type.FOLLOWING, new User(1));
        expect(a1).toEqual(a2);

        a2 = new UserAssociation(UserAssociation.Type.FOLLOWER, new User(2));
        expect(a1).not.toEqual(a2);

        a2 = null;
        expect(a1).not.toEqual(a2);
    }

    @Test
    public void testShouldBeMarkedForAddition() {
        UserAssociation association = new UserAssociation(UserAssociation.Type.FOLLOWING, new User(1L));
        association.markForAddition();
        expect(association.isMarkedForAddition()).toBeTrue();
    }

    @Test
    public void testShouldBeMarkedForRemoval() {
        UserAssociation association = new UserAssociation(UserAssociation.Type.FOLLOWING, new User(1L));
        association.markForRemoval();
        expect(association.isMarkedForRemoval()).toBeTrue();
    }

    @Test
    public void testShouldNotBeMarkedForAdditionAfterMarkedForRemoval() {
        UserAssociation association = new UserAssociation(UserAssociation.Type.FOLLOWING, new User(1L));
        association.markForAddition();
        expect(association.isMarkedForAddition()).toBeTrue();
        association.markForRemoval();
        expect(association.isMarkedForAddition()).toBeFalse();
    }

    @Test
    public void testShouldNotBeMarkedForRemovalAfterMarkedForAddition() {
        UserAssociation association = new UserAssociation(UserAssociation.Type.FOLLOWING, new User(1L));
        association.markForRemoval();
        expect(association.isMarkedForRemoval()).toBeTrue();
        association.markForAddition();
        expect(association.isMarkedForRemoval()).toBeFalse();
    }


    private void compareUserAssociations(UserAssociation userAssociation, UserAssociation userAssociation2) {
        expect(userAssociation2.id).toEqual(userAssociation.id);
        expect(userAssociation2.created_at).toEqual(userAssociation.created_at);
        expect(userAssociation2.associationType).toEqual(userAssociation.associationType);
        expect(userAssociation2.getUser()).toEqual(userAssociation.getUser());
    }
}
