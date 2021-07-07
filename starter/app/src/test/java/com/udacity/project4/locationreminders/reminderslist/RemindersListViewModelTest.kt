package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class RemindersListViewModelTest {

    private lateinit var dataSource: FakeDataSource

    private lateinit var remindersViewModel: RemindersListViewModel

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        dataSource = FakeDataSource()
        remindersViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }

    @Test
    fun newViewModel_loadReminders_notEmpty() = mainCoroutineRule.runBlockingTest {
        // GIVEN - A fresh view model with a reminder
        val reminder = ReminderDTO("Grab Burger", null, "Green Bay", 32.02, 34.02)
        dataSource.saveReminder(reminder)
        // WHEN - loading Reminders
        mainCoroutineRule.pauseDispatcher()
        remindersViewModel.loadReminders()

        val loadingValue = remindersViewModel.showLoading.getOrAwaitValue()
        assertThat(loadingValue, `is`(true))

        mainCoroutineRule.resumeDispatcher()
        val reminders = remindersViewModel.remindersList.getOrAwaitValue()
        val showNoDataValue = remindersViewModel.showNoData.getOrAwaitValue()
        // THEN -  No Data Value should be false if the value is greater than 0
        assertThat(
            reminders.size, (not(nullValue()))
        )
        assertThat(
            reminders.size, `is`(greaterThan(0))
        )
        assertThat(
            showNoDataValue, `is`(false)
        )
    }

    @Test
    fun newViewModel_loadReminders_empty() {
        // GIVEN - a Fresh view model with no reminders
        // WHEN - After loading Reminders and it returns an empty list
        remindersViewModel.loadReminders()
        val reminders = remindersViewModel.remindersList.getOrAwaitValue()
        val showNoDataValue = remindersViewModel.showNoData.getOrAwaitValue()
        // THEN - The Reminders list size should be 0 and showNoData value should be true
        assertThat(
            reminders.size, `is`(0)
        )
        assertThat(
            showNoDataValue, `is`(true)
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}