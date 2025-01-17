package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.FakeAndroidTestDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.ToastMatcher
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.inject
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.koin.test.inject


@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    private lateinit var reminderListViewModel: RemindersListViewModel

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()
        reminderListViewModel = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    //    TODO: add End to End testing to the app
    @Test
    fun addReminder() = runBlocking {
        // GIVEN - A user wants to add a reminder
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        // WHEN -
        onView(withId(R.id.map)).check(matches(isDisplayed()))
        onView(withId(R.id.map)).perform(longClick())

        onView(withId(R.id.save_poi_button)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(replaceText("Test"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Test Description"))

        onView(withId(R.id.saveReminder)).perform(click())
        onView(withText(R.string.reminder_saved)).inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
        activityScenario.close()
    }

    @Test
    fun addReminder_noTitle() = runBlocking {
        // GIVEN - A user wants to add a reminder
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        // WHEN -
        onView(withId(R.id.map)).check(matches(isDisplayed()))
        onView(withId(R.id.map)).perform(longClick())

        onView(withId(R.id.save_poi_button)).perform(click())
        onView(withId(R.id.reminderDescription)).perform(replaceText("Test Description"))

        onView(withId(R.id.saveReminder)).perform(click())
        onView(withText(R.string.err_enter_title))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        activityScenario.close()
    }

    @Test
    fun addReminder_noLocation() = runBlocking {
        // GIVEN - A user wants to add a reminder
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(R.id.addReminderFAB)).perform(click())
        // WHEN - NO Location is Selected

        onView(withId(R.id.reminderTitle)).perform(replaceText("Title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Test Description"))

        onView(withId(R.id.saveReminder)).perform(click())
        // THEN - A snackbar should show with "Please select a location"
        onView(withText(R.string.err_select_location))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        activityScenario.close()
    }

    /**
     * Impossible for this test to pass without using a Fake Data Source
     * as I have no way os consistently making the loadReminders call fail
     * without changing the code
     */
    @Ignore("Test Won't pass unless using a FakeAndroidTestDataSource as I can't consistently force a DB error when loading Reminders")
    @ExperimentalCoroutinesApi
    @Test
    fun failedToLoadReminders() = runBlocking {
        // GIVEN - A user wants to add a reminder
//        repository.setReturnError(true)
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        // WHEN - When the reminders fail to load
        // THEN - There should be a snackbar displayed with "Reminders Not Found"
        onView(withText("Reminders Not Found"))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        activityScenario.close()
    }
}
