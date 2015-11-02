package com.soundcloud.android.comments;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiComment;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class StoreCommentCommandTest extends StorageIntegrationTest {

    private StoreCommentCommand command;

    @Before
    public void setUp() throws Exception {
        command = new StoreCommentCommand(propeller());
    }

    @Test
    public void shouldStoreCommenters() {
        CommentRecord comment = apiComment(Urn.forComment(123));

        command.call(comment);

        databaseAssertions().assertUserInserted(comment.getUser());
    }

    @Test
    public void shouldStoreComments() {
        CommentRecord comment = apiComment(Urn.forComment(123));

        command.call(comment);

        assertThat(command.lastRowId()).isGreaterThan(0);
        databaseAssertions().assertCommentInserted(comment);
    }
}
