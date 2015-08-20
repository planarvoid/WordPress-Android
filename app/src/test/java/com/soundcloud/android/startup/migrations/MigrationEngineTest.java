package com.soundcloud.android.startup.migrations;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.SharedPreferences;

@RunWith(MockitoJUnitRunner.class)
public class MigrationEngineTest {

    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor editor;
    @Mock
    private Migration migrationOne;
    @Mock
    private Migration migrationTwo;

    @Before
    public void setUp() {
        when(sharedPreferences.edit()).thenReturn(editor);
    }

    @Test
    public void shouldNotPerformMigrationIfNoPreviousVersionCodeExistsInSharedPreferences() {
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(-1);
        new MigrationEngine(12, sharedPreferences, migrationOne, migrationTwo).migrate();
        verifyZeroInteractions(migrationOne, migrationTwo);
    }

    @Test
    public void shouldNotPerformMigrationIfThePreviousAppVersionCodeIsTheSameAsTheCurrentAppVersionCode() {
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(44);
        new MigrationEngine(44, sharedPreferences, migrationOne, migrationTwo).migrate();
        verifyZeroInteractions(migrationOne, migrationTwo);
    }

    @Test
    public void shouldNotPerformMigrationIfApplicableVersionCodeForMigrationsIsLargerThanTheCurrentAppVersionCode() {
        when(migrationOne.getApplicableAppVersionCode()).thenReturn(55);
        when(migrationTwo.getApplicableAppVersionCode()).thenReturn(56);
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(40);
        new MigrationEngine(54, sharedPreferences, migrationOne, migrationTwo).migrate();
        verify(migrationOne, never()).applyMigration();
        verify(migrationTwo, never()).applyMigration();
    }

    @Test
    public void shouldPerformMigrationIfApplicableVersionCodeForMigrationsIsEqualToTheCurrentAppVersionCodeButGreaterThanThePreviousAppVersionCode() {
        when(migrationOne.getApplicableAppVersionCode()).thenReturn(56);
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(54);
        new MigrationEngine(56, sharedPreferences, migrationOne).migrate();
        verify(migrationOne).applyMigration();
    }

    @Test
    public void shouldPerformMigrationIfApplicableVersionCodeForMigrationsIsLessThanTheCurrentAppVersionCodeButGreaterThanThePreviousAppVersionCode() {
        when(migrationOne.getApplicableAppVersionCode()).thenReturn(56);
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(54);
        new MigrationEngine(57, sharedPreferences, migrationOne).migrate();
        verify(migrationOne).applyMigration();
    }

    @Test
    public void shouldNotPerformMigrationIfApplicableVersionCodeForMigrationsIsLessThanTheCurrentAppVersionCodeButEqualToThePreviousAppVersionCode() {
        when(migrationOne.getApplicableAppVersionCode()).thenReturn(54);
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(54);
        new MigrationEngine(57, sharedPreferences, migrationOne).migrate();
        verify(migrationOne, never()).applyMigration();
    }

    @Test
    public void shouldSortTheMigrationsToApplyBasedOnApplicableVersionCode() {
        when(migrationOne.getApplicableAppVersionCode()).thenReturn(54);
        when(migrationTwo.getApplicableAppVersionCode()).thenReturn(53);
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(52);
        new MigrationEngine(57, sharedPreferences, migrationOne, migrationTwo).migrate();
        InOrder inOrder = inOrder(migrationOne, migrationTwo);
        inOrder.verify(migrationTwo).applyMigration();
        inOrder.verify(migrationOne).applyMigration();
    }

    @Test
    public void shouldUpdateTheCurrentVersionCodeAfterMigrations() {
        when(migrationOne.getApplicableAppVersionCode()).thenReturn(56);
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(54);
        new MigrationEngine(57, sharedPreferences, migrationOne).migrate();
        InOrder inOrder = inOrder(migrationOne, editor);
        inOrder.verify(migrationOne).applyMigration();
        inOrder.verify(editor).putInt("changeLogVersionCode", 57);
        inOrder.verify(editor).apply();
    }

}
