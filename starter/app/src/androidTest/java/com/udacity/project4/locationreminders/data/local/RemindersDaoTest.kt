package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun cleanUpDb() {
        database.close()
    }

    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        // GIVEN - Insert a reminder
        val reminder = ReminderDTO("Grab Burger", null, "Green Bay", 32.02, 34.02)
        database.reminderDao().saveReminder(reminder)

        // WHEN - Get the reminder by id from the database
        val fetchedReminder = database.reminderDao().getReminderById(reminder.id)

        // THEN - The loaded data contains expected values
        assertThat<ReminderDTO>(fetchedReminder as ReminderDTO, notNullValue())
        assertThat(fetchedReminder.id, `is`(reminder.id))
        assertThat(fetchedReminder.title, `is`(reminder.title))
        assertThat(fetchedReminder.description, `is`(reminder.description))
        assertThat(fetchedReminder.latitude, `is`(reminder.latitude))
        assertThat(fetchedReminder.longitude, `is`(reminder.longitude))
        assertThat(fetchedReminder.location, `is`(reminder.location))
    }

    @Test
    fun insertReminderAndDeleteAll() = runBlockingTest {
        // GIVEN - Insert a reminder
        val reminder = ReminderDTO("Grab Burger", null, "Green Bay", 32.02, 34.02)
        database.reminderDao().saveReminder(reminder)

        // WHEN - Delete all reminders from the database and fetch
        database.reminderDao().deleteAllReminders()
        val reminders = database.reminderDao().getReminders()

        // THEN - Reminders size should be 0
        assertThat(reminders.size, `is`(0))
    }

    @Test
    fun addRemindersAndFetchAll() = runBlockingTest {
        // GIVEN - A set of reminders are inserted into database
        val reminder = ReminderDTO("Grab Burger", null, "Green Bay", 32.02, 34.02)
        database.reminderDao().saveReminder(reminder)
        val reminder2 = ReminderDTO("Grab Burger", null, "Green Bay", 32.02, 34.02)
        database.reminderDao().saveReminder(reminder2)
        val reminder3 = ReminderDTO("Grab Burger", null, "Green Bay", 32.02, 34.02)
        database.reminderDao().saveReminder(reminder3)

        // WHEN - All Reminders are fetched
        val reminders = database.reminderDao().getReminders()

        // THEN - Reminders size should be greater than 0
        assertThat(reminders.size, `is`(greaterThan(0)))
    }

}