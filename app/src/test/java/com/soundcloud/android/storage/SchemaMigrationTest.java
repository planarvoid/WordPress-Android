package com.soundcloud.android.storage;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.DatabaseMigrationHelper;
import org.junit.Before;
import org.junit.Test;

// Thanks Jeremie! http://jeremie-martinez.com/2016/02/16/unit-tests/

public class SchemaMigrationTest extends AndroidUnitTest {

    private DatabaseMigrationHelper migrationHelper;

    @Before
    public void setUp() throws Exception {
        migrationHelper = new DatabaseMigrationHelper();
    }

    @Test
    public void shouldBeExactlyTheSameSchemaWhenUpgradingAndWhenRecreating() {
        migrationHelper.upgradeToCurrent();
        migrationHelper.assertSchemas();
    }
}
