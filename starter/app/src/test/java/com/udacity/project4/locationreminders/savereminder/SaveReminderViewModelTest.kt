package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
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
class SaveReminderViewModelTest {

    private lateinit var dataSource: FakeDataSource

    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    //TODO: provide testing to the SaveReminderView and its live data objects
    @Before
    fun setupViewModel() {
        dataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }

    @Test
    fun newViewModel_saveValidReminder() = mainCoroutineRule.runBlockingTest {
        // GIVEN - A fresh Save Reminder View Model
        // WHEN - A Reminder is saved
        val reminder = ReminderDataItem("Grab Burger", null, "Green Bay", 32.02, 34.02)
        saveReminderViewModel.validateAndSaveReminder(reminder)
        // THEN - Check data source for new reminder
        val retrievedReminder = dataSource.getReminder(reminder.id) as Result.Success
        val toastValue = saveReminderViewModel.showToast.getOrAwaitValue()
        val loadingValue = saveReminderViewModel.showLoading.getOrAwaitValue()
        assertThat(retrievedReminder.data.title, `is`(reminder.title))
        assertThat(retrievedReminder.data.description, `is`(reminder.description))
        assertThat(retrievedReminder.data.location, `is`(reminder.location))
        assertThat(retrievedReminder.data.latitude, `is`(reminder.latitude))
        assertThat(retrievedReminder.data.longitude, `is`(reminder.longitude))
        assertThat(toastValue, `is`("Reminder Saved !"))
        assertThat(loadingValue, `is`(false))
    }

    @Test
    fun newViewModel_saveInvalidTitleReminder() = mainCoroutineRule.runBlockingTest {
        // GIVEN - A Fresh view model
        // WHEN - A invalid reminder is attempted to be saved
        val reminder = ReminderDataItem(null, null, "Green Bay", 32.02, 34.02)
        saveReminderViewModel.validateAndSaveReminder(reminder)
        // THEN - Snackbar is displayed with an error
        val snackBarValueInt = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(snackBarValueInt, `is`(R.string.err_enter_title))
    }

    @Test
    fun newViewModel_saveInvalidLocationReminder() = mainCoroutineRule.runBlockingTest {
        // GIVEN - A Fresh view model
        // WHEN - A invalid reminder is attempted to be saved
        val reminder = ReminderDataItem("Get Burger", null, null, 32.02, 34.02)
        saveReminderViewModel.validateAndSaveReminder(reminder)
        // THEN - Snackbar is displayed with an error
        val snackBarValueInt = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(snackBarValueInt, `is`(R.string.err_select_location))
    }

    @Test
    fun newViewModel_saveValidPoiReminder() = mainCoroutineRule.runBlockingTest {
        // GIVEN - A Fresh view model
        // WHEN - A Valid reminder is attempted to be saved
        val poi = LatLng(32.02, 34.02)
        val reminder = PointOfInterest(poi, "Burger", "Burger King")
        saveReminderViewModel.validatePoiSelected(reminder)
        // THEN - Selected POI
        val selectedPoi = saveReminderViewModel.selectedPOI.getOrAwaitValue()
        val selectedLocationString = saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue()
        assertThat(selectedPoi, `is`(notNullValue()))
        assertThat(selectedPoi.name, `is`("Burger King"))
        assertThat(selectedPoi.latLng, `is`(LatLng(32.02, 34.02)))
        assertThat(selectedLocationString, `is`(selectedPoi.name))
    }

    @Test
    fun newViewModel_saveInvalidPoiReminder() = mainCoroutineRule.runBlockingTest {
        // GIVEN - A Fresh view model
        // WHEN - A invalid reminder is attempted to be saved
        saveReminderViewModel.validatePoiSelected(null)
        // THEN - Selected POI
        val showToastValue = saveReminderViewModel.showToast.getOrAwaitValue()
        assertThat(showToastValue, `is`("Please select a point of interest"))
    }

    @After
    fun tearDown() {
        stopKoin()
    }

}