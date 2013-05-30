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
        UserAssociation userAssociation = new UserAssociation(u, Association.Type.FOLLOWING,new Date());

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
        UserAssociation UserAssociation1 = new UserAssociation(u, Association.Type.FOLLOWING, new Date());
        UserAssociation UserAssociation2 = new UserAssociation(u, Association.Type.FOLLOWER, new Date());
        expect(UserAssociation1.getListItemId()).not.toEqual(UserAssociation2.getListItemId());
    }

    @Test
    public void testEquals() {
        UserAssociation a1 = new UserAssociation(new User(1L), UserAssociation.Type.FOLLOWING, new Date());
        UserAssociation a2;

        a2 = new UserAssociation(new User(1), UserAssociation.Type.FOLLOWING, new Date());
        expect(a1).toEqual(a2);

        a2 = new UserAssociation(new User(2), UserAssociation.Type.FOLLOWER, new Date());
        expect(a1).not.toEqual(a2);

        a2 = null;
        expect(a1).not.toEqual(a2);
    }

    private void compareUserAssociations(UserAssociation userAssociation, UserAssociation userAssociation2) {
        expect(userAssociation2.id).toEqual(userAssociation.id);
        expect(userAssociation2.created_at).toEqual(userAssociation.created_at);
        expect(userAssociation2.associationType).toEqual(userAssociation.associationType);
        expect(userAssociation2.getUser()).toEqual(userAssociation.getUser());
    }
}
