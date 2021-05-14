package com.udacity.asteroidradar.data.repository

import android.annotation.SuppressLint
import com.udacity.asteroidradar.data.api.Network
import com.udacity.asteroidradar.data.api.parseAsteroidsJsonResult
import com.udacity.asteroidradar.data.constants.Constants
import com.udacity.asteroidradar.data.models.Asteroid
import com.udacity.asteroidradar.data.persistence.AsteroidDatabase
import com.udacity.asteroidradar.data.persistence.entities.DatabaseAsteroid
import com.udacity.asteroidradar.data.persistence.entities.asDatabaseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class AsteroidRepository(private val database: AsteroidDatabase) {
    @SuppressLint("WeekBasedYear")
    private val dateFormatter =
        DateTimeFormatter.ofPattern(Constants.API_QUERY_DATE_FORMAT, Locale.getDefault())

    fun getAsteroidsAll(): Flow<List<DatabaseAsteroid>> =
        database.asteroidDao.getAsteroids()

    fun getAsteroidsToday(
        startDate: String
    ): Flow<List<DatabaseAsteroid>> =
        database.asteroidDao.getAsteroidsForDate(startDate)

    fun getAsteroidsWeekly(
        startDate: String,
        endDate: String
    ): Flow<List<DatabaseAsteroid>> =
        database.asteroidDao.getAsteroidsBetweenDates(startDate, endDate)


    suspend fun refreshAsteroids(
    ) {
        val startDate = dateFormatter.format(LocalDate.now())
        val endDate =
            dateFormatter.format(LocalDate.now().plusDays(Constants.DEFAULT_END_DATE_DAYS.toLong()))
        var asteroidList: ArrayList<Asteroid>
        withContext(Dispatchers.IO) {
            val asteroidResponseBody: ResponseBody = Network.service.getAsteroidsAsync(
                startDate, endDate,
                Constants.API_KEY
            )
                .await()
            asteroidList = parseAsteroidsJsonResult(JSONObject(asteroidResponseBody.string()))
            database.asteroidDao.insertAll(*asteroidList.asDatabaseModel())
        }
    }

    suspend fun deletePreviousDayAsteroids() {
        withContext(Dispatchers.IO) {
            database.asteroidDao.deletePreviousDayAsteroids(dateFormatter.format(LocalDate.now()))
        }
    }
}