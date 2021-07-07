package com.udacity.project4.locationreminders.reminderslist

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.MyApp
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidTestDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import javax.inject.Inject

//UI Testing
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest: KoinTest {
    private lateinit var application: MyApp
    private lateinit var repository: FakeAndroidTestDataSource
    private lateinit var viewModel: RemindersListViewModel
    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        stopKoin()
        application = getApplicationContext()
        repository = FakeAndroidTestDataSource()
        viewModel = RemindersListViewModel(application, repository)
        val myModule = module {
            viewModel{
                RemindersListViewModel(application, repository)
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(application)}
        }
        startKoin {
            modules(listOf(myModule))
        }
    }


    @After
    fun tearDown() {
        stopKoin()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun populatedReminderList_DisplayedInUI() = runBlockingTest {
        // GIVEN - A List of Reminders
        val reminder = ReminderDTO("Get Burger", null, "Green Bay", 53.02, 51.02)
        val reminder1 = ReminderDTO("Get Fries", null, "Green Bay", 53.02, 51.02)
        repository.saveReminder(reminder)
        repository.saveReminder(reminder1)
        // WHEN - RemindersListFragment is launched to display list
        launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        // THEN - Verify Reminders are displayed
        onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))
        onView(withText(reminder.title)).check(matches(isDisplayed()))
        onView(withText(reminder1.title)).check(matches(isDisplayed()))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun emptyRemindersList_NoDataDisplayedInUI() {
        // GIVEN - Given an empty recycler view in the Reminder List Fragment
        // WHEN - Reminders List Frag is launched
        launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        // THEN - No data should be displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun clickFab_NavigateToSaveReminderFragment() {
        // GIVEN - On the RemindersListFragment Screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // WHEN - Floating Action Button to add reminder is clicked
        onView(withId(R.id.addReminderFAB))
            .perform(click())
        // THEN - Verify that we navigate to save reminder screen
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }
}