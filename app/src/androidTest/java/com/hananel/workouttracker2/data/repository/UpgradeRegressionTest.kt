package com.hananel.workouttracker2.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.DisplayNames
import com.hananel.workouttracker2.data.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UpgradeRegressionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val file = File(context.cacheDir, "upgrade-${UUID.randomUUID()}.preferences_pb")
    private val store = PreferenceDataStoreFactory.create(scope = scope) { file }
    private val repository = WorkoutRepository(context, store)

    @After fun cleanUp() { scope.cancel(); file.delete() }

    @Test fun resourceIdsFromShipped22ResolveAfterResourcesChange() = runBlocking {
        val original = """[{"id":"day_0","nameResId":2131361845,"dayIndex":0,"customName":"אימון A","exercises":[{"id":"1","nameResId":2131361873,"sets":4,"reps":"10","restTime":60}]}]"""
        store.edit { it[stringPreferencesKey("workout_structure")] = original }
        val day = repository.getWorkoutStructure().single()
        assertEquals("אימון A", day.getDisplayName(context))
        assertEquals("exercise1", day.exercises.single().nameKey)
        assertEquals(context.getString(R.string.exercise1), day.exercises.single().getDisplayName(context))
        repository.saveWorkoutStructure(listOf(day))
        assertEquals(day, repository.getWorkoutStructure().single())
    }

    @Test fun unknownAndCustomNamesDoNotErasePlansWeightsOrHistory() = runBlocking {
        val day = WorkoutDay("day", 999999, dayIndex = 0, exercises = listOf(
            Exercise("custom", 0, customName = "תרגיל שלי", isCustom = true, sets = 3, reps = "10", restTime = 30),
            Exercise("unknown", 999999, sets = 2, reps = "8", restTime = 45)))
        repository.saveWorkoutStructure(listOf(day))
        repository.updateExerciseWeight("custom", 22.5f)
        val restored = repository.getWorkoutStructure().single()
        assertEquals(2, restored.exercises.size)
        assertEquals("תרגיל שלי", restored.exercises.first().getDisplayName(context))
        assertTrue(restored.exercises.last().getDisplayName(context).isNotBlank())
        assertEquals(22.5f, repository.getWorkoutProgress().exerciseWeights["custom"]!!, .001f)
    }

    @Test fun pre22DefaultProgramUsesStableIdentitiesEvenWhenOldIdsPointAtOtherStrings() = runBlocking {
        val original = """[{"id":"day_0","nameResId":2131362186,"dayIndex":0,"exercises":[{"id":"1","nameResId":2131361870,"sets":5,"reps":"10-12","restTime":60},{"id":"2","nameResId":2131361881,"sets":4,"reps":"12-15","restTime":45}]}]"""
        store.edit { it[stringPreferencesKey("workout_structure")] = original }
        val day = repository.getWorkoutStructure().single()
        assertEquals(context.getString(R.string.workout_chest_shoulders), day.getDisplayName(context))
        assertEquals(context.getString(R.string.exercise1), day.exercises[0].getDisplayName(context))
        assertEquals(context.getString(R.string.exercise2), day.exercises[1].getDisplayName(context))
    }

    @Test fun corruptPlanIsReportedAndItsOriginalBytesAreRetained() = runBlocking {
        val key = stringPreferencesKey("workout_structure")
        val broken = "[{unreadable"
        store.edit { it[key] = broken }
        assertTrue(runCatching { repository.getWorkoutStructure() }.exceptionOrNull() is WorkoutDataException)
        assertTrue(runCatching { repository.updateWorkoutStructure { emptyList() } }.isFailure)
        assertEquals(broken, store.data.first()[key])
    }

    @Test fun concurrentSettingsChangesPreserveBothFields() = runBlocking {
        coroutineScope {
            launch { repository.updateUserPreferences { it.copy(theme = "DARK") } }
            launch { repository.updateLastViewedDay(3) }
            launch { repository.updateUserPreferences { it.copy(language = "he") } }
        }
        val prefs = repository.getUserPreferences()
        assertEquals("DARK", prefs.theme)
        assertEquals("he", prefs.language)
        assertEquals(3, prefs.lastViewedDayIndex)
    }

    @Test fun concurrentPlanChangesPreserveBothExercises() = runBlocking {
        repository.saveWorkoutStructure(listOf(WorkoutDay("day", R.string.day_sunday, dayIndex = 0, exercises = emptyList())))
        coroutineScope {
            repeat(2) { index -> launch {
                repository.updateWorkoutStructure { days -> days.map { day -> day.copy(exercises = day.exercises +
                    Exercise("ex$index", 0, customName = "Custom $index", sets = 3, reps = "10", restTime = 0)) } }
            } }
        }
        assertEquals(2, repository.getWorkoutStructure().single().exercises.size)
    }

    @Test fun extraCompletionDoesNotRewriteRepsOrReopenArchivedSession() = runBlocking {
        val ctx = SessionContext(0, exerciseName = "Press", reps = "10")
        repository.completeSet("ex", 1, ctx, reps = 9)
        repository.endActiveSession()
        repository.completeSet("ex", 1, ctx, reps = 99)
        assertNull(repository.getActiveSession())
        assertEquals(listOf(9), repository.getSessionHistory().single().exercises.single().repsPerSet)
    }
}
