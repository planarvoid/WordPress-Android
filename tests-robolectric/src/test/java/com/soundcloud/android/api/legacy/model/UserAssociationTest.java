package com.soundcloud.android.api.legacy.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class UserAssociationTest {

    public static final String TOKEN = "12345";

    @Test
    public void shouldParcelAndUnparcelCorrectly() throws Exception {
        PublicApiUser u = new PublicApiUser(123L);
        UserAssociation userAssociation = new UserAssociation(Association.Type.FOLLOWING, u);

        expect(userAssociation.associationType).not.toBeNull();
        expect(userAssociation.created_at).not.toBeNull();
        expect(userAssociation.getUser()).not.toBeNull();

        Parcel p = Parcel.obtain();
        userAssociation.writeToParcel(p, 0);
        final UserAssociation userAssociation2 = new UserAssociation(p);
        compareUserAssociations(userAssociation2, userAssociation);
        expect(userAssociation2.getToken()).toBeNull();
    }

    @Test
    public void shouldParcelAndUnparcelWithAddition() throws Exception {
        PublicApiUser u = new PublicApiUser(123L);
        UserAssociation userAssociation = new UserAssociation(Association.Type.FOLLOWING, u);
        userAssociation.markForAddition(TOKEN);

        Parcel p = Parcel.obtain();
        userAssociation.writeToParcel(p, 0);
        final UserAssociation userAssociation2 = new UserAssociation(p);
        compareUserAssociations(userAssociation2, userAssociation);
        expect(userAssociation.getToken()).toEqual(userAssociation2.getToken());
    }

    @Test
    public void shouldProvideUniqueListItemId() throws Exception {
        PublicApiUser u = new PublicApiUser(123L);
        UserAssociation UserAssociation1 = new UserAssociation(Association.Type.FOLLOWING, u);
        UserAssociation UserAssociation2 = new UserAssociation(Association.Type.FOLLOWER, u);
        expect(UserAssociation1.getListItemId()).not.toEqual(UserAssociation2.getListItemId());
    }

    @Test
    public void testEquals() {
        UserAssociation a1 = new UserAssociation(UserAssociation.Type.FOLLOWING, new PublicApiUser(1L));
        UserAssociation a2;

        a2 = new UserAssociation(UserAssociation.Type.FOLLOWING, new PublicApiUser(1));
        expect(a1).toEqual(a2);

        a2 = new UserAssociation(UserAssociation.Type.FOLLOWER, new PublicApiUser(2));
        expect(a1).not.toEqual(a2);

        a2 = null;
        expect(a1).not.toEqual(a2);
    }

    @Test
    public void testShouldBeMarkedForAddition() {
        UserAssociation association = new UserAssociation(UserAssociation.Type.FOLLOWING, new PublicApiUser(1L));
        association.markForAddition(TOKEN);
        expect(association.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);
        expect(association.getToken()).toEqual(TOKEN);
    }

    @Test
    public void testShouldBeMarkedForRemoval() {
        UserAssociation association = new UserAssociation(UserAssociation.Type.FOLLOWING, new PublicApiUser(1L));
        association.markForRemoval();
        expect(association.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_REMOVAL);
    }

    @Test
    public void testShouldNotBeMarkedForAdditionAfterMarkedForRemoval() {
        UserAssociation association = new UserAssociation(UserAssociation.Type.FOLLOWING, new PublicApiUser(1L));
        association.markForAddition(TOKEN);
        expect(association.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);
        association.markForRemoval();
        expect(association.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_REMOVAL);
    }

    @Test
    public void testShouldNotBeMarkedForAdditionAfterLocalSyncStateCleared() {
        UserAssociation association = new UserAssociation(UserAssociation.Type.FOLLOWING, new PublicApiUser(1L));
        association.markForAddition(TOKEN);
        expect(association.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);
        association.clearLocalSyncState();
        expect(association.getLocalSyncState()).toEqual(UserAssociation.LocalState.NONE);
    }

    private void compareUserAssociations(UserAssociation userAssociation, UserAssociation userAssociation2) {
        expect(userAssociation2.getId()).toEqual(userAssociation.getId());
        expect(userAssociation2.created_at).toEqual(userAssociation.created_at);
        expect(userAssociation2.associationType).toEqual(userAssociation.associationType);
        expect(userAssociation2.getLocalSyncState()).toEqual(userAssociation.getLocalSyncState());
        expect(userAssociation2.getUser()).toEqual(userAssociation.getUser());
    }
}
