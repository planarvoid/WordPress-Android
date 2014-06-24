package com.soundcloud.android.model.activities;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.ActivityProperty;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class CommentActivityTest {

    @Test
    public void shouldConvertToPropertySet() throws CreateModelException {
        CommentActivity activity = new CommentActivity();
        activity.comment = TestHelper.getModelFactory().createModel(Comment.class);
        activity.createdAt = new Date();

        final PropertySet propertySet = activity.toPropertySet();

        expect(propertySet.get(ActivityProperty.TYPE)).toEqual(ActivityProperty.TYPE_COMMENT);
        expect(propertySet.get(ActivityProperty.USER_NAME)).toEqual(activity.getUser().getUsername());
        expect(propertySet.get(ActivityProperty.DATE)).toEqual(activity.getCreatedAt());
        expect(propertySet.get(ActivityProperty.SOUND_TITLE)).toEqual(activity.getPlayable().getTitle());
        expect(propertySet.get(ActivityProperty.USER_URN)).toEqual(activity.getUser().getUrn());
    }


}