package com.udacity.project4.locationreminders.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.lang.Exception

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    private lateinit var reminderDao: RemindersDao
    private lateinit var db: RemindersDatabase

    private lateinit var remindersLocalRepository: RemindersLocalRepository

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RemindersDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reminderDao = db.reminderDao()
    }

    @Before
    fun createRepository() {
        remindersLocalRepository = RemindersLocalRepository(reminderDao, Dispatchers.Main)
    }

    @Test
    @ExperimentalCoroutinesApi
    @Throws(Exception::class)
    fun getReminders_requestAllRemindersFromLocalDataSource() = mainCoroutineRule.runBlockingTest {
        // GIVEN - A reminder is added to the repository
        val reminder = ReminderDTO("Grab Burger", null, "Green Bay", 32.02, 34.02)
        remindersLocalRepository.saveReminder(reminder)
        // WHEN - reminders are requested from repository
        val reminders = remindersLocalRepository.getReminders() as Result.Success
        // THEN -  reminders are loaded from the local data source
        assertThat(reminders.data, `is`(notNullValue()))
    }

    @Test
    @ExperimentalCoroutinesApi
    fun getRemindersById_returnsError() = mainCoroutineRule.runBlockingTest {
        // GIVEN - Given an empty list of reminders
        // WHEN - A reminder is attempted to be fetched
        val fetchedReminder = remindersLocalRepository.getReminder("1") as Result.Error
        // THEN - An error message is returned
        assertThat(fetchedReminder.message, `is`("Reminder not found!"))
    }

    @Test
    @ExperimentalCoroutinesApi
    fun addReminder_toLocalDataSource() = mainCoroutineRule.runBlockingTest {
        // GIVEN - A new reminder is saved to the database
        val reminder = ReminderDTO("Grab Burger", null, "Green Bay", 32.02, 34.02)
        remindersLocalRepository.saveReminder(reminder)
        // WHEN - Reminder is retrieved
        val retrievedReminder = remindersLocalRepository.getReminder(reminder.id) as Result.Success
        // THEN - reminder fetched is equal to reminder created
        assertThat(retrievedReminder.data.title, `is`(reminder.title))
        assertThat(retrievedReminder.data.description, `is`(reminder.description))
        assertThat(retrievedReminder.data.location, `is`(reminder.location))
        assertThat(retrievedReminder.data.latitude, `is`(reminder.latitude))
        assertThat(retrievedReminder.data.longitude, `is`(reminder.longitude))
    }

    @Test
    @ExperimentalCoroutinesApi
    fun deleteReminders_fromLocalDataSource() = mainCoroutineRule.runBlockingTest {
        // GIVEN - Reminders added to the repository
        val reminder = ReminderDTO("Grab Burger", null, "Green Bay", 32.02, 34.02)
        remindersLocalRepository.saveReminder(reminder)
        // WHEN -  All Are deleted
        remindersLocalRepository.deleteAllReminders()
        // THEN - An empty list should be returned
        val reminders = remindersLocalRepository.getReminders() as Result.Success

        assertThat(reminders.data, `is`(emptyList()))
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        mainCoroutineRule.runBlockingTest {
            reminderDao.deleteAllReminders()
        }
        db.close()
    }
}