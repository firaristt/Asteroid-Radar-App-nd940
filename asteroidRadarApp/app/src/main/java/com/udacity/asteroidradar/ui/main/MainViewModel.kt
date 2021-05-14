package com.udacity.asteroidradar.ui.main

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.udacity.asteroidradar.data.api.Network
import com.udacity.asteroidradar.data.constants.Constants
import com.udacity.asteroidradar.data.models.Asteroid
import com.udacity.asteroidradar.data.models.PictureOfDay
import com.udacity.asteroidradar.data.persistence.entities.asDomainModel
import com.udacity.asteroidradar.data.persistence.getDatabase
import com.udacity.asteroidradar.data.repository.AsteroidRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val asteroidRepository = AsteroidRepository(getDatabase(application))

    @SuppressLint("WeekBasedYear")
    private val dateFormatter =
        DateTimeFormatter.ofPattern(Constants.API_QUERY_DATE_FORMAT, Locale.getDefault())

    private fun getToday(): String = dateFormatter.format(LocalDate.now())
    private fun getEndDay(): String =
        dateFormatter.format(LocalDate.now().plusDays(Constants.DEFAULT_END_DATE_DAYS.toLong()))

    private val _navigateToDetailFragment = MutableLiveData<Asteroid?>()
    val navigateToDetailFragment: MutableLiveData<Asteroid?>
        get() = _navigateToDetailFragment

    private var _asteroids = MutableLiveData<List<Asteroid>>()
    val asteroids: LiveData<List<Asteroid>>
        get() = _asteroids

    private val _pictureOfDay = MutableLiveData<PictureOfDay>()
    val pictureOfDay: LiveData<PictureOfDay>
        get() = _pictureOfDay

    init {
        onViewWeekClicked()
        viewModelScope.launch {
            try {
                asteroidRepository.refreshAsteroids()
                refreshPictureOfDay()
            } catch (e: Exception) {
                println("Error while refreshing data: $e.message}")
                // Better to use sth like Timber
            }
        }
    }

    private suspend fun refreshPictureOfDay() {
        val pictureOfDay: PictureOfDay
        withContext(Dispatchers.Main) {
            pictureOfDay = Network.service.getPictureOfDayAsync().await()
            if (pictureOfDay.mediaType == "image")
                _pictureOfDay.value = pictureOfDay
        }

    }

    fun onAsteroidClicked(asteroid: Asteroid) {
        _navigateToDetailFragment.value = asteroid
    }

    fun doneNavigating() {
        _navigateToDetailFragment.value = null
    }

    @InternalCoroutinesApi
    fun onSavedAsteroidsClicked() {
        viewModelScope.launch {
            getAllAsteroids()
        }
    }

    private suspend fun getAllAsteroids() {
        try {
            asteroidRepository.getAsteroidsAll().distinctUntilChanged().filterNotNull()
                .map { it.asDomainModel() }.collect { asteroids ->
                    _asteroids.value = asteroids
                }
        } catch (e: Exception) {
            println("Error while setting asteroids ${e.message}")
            // Better to use sth like Timber
        }
    }

    fun onTodayClicked() {
        viewModelScope.launch {
            asteroidRepository.getAsteroidsToday(getToday()).distinctUntilChanged()
                .filterNotNull()
                .map { it.asDomainModel() }.collect { asteroids ->
                    _asteroids.value = asteroids
                }
        }
    }

    fun onViewWeekClicked() {
        viewModelScope.launch {
            asteroidRepository.getAsteroidsWeekly(getToday(), getEndDay()).distinctUntilChanged()
                .filterNotNull()
                .map { it.asDomainModel() }.collect { asteroids ->
                    _asteroids.value = asteroids
                }
        }
    }
}