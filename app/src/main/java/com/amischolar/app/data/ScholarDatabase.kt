package com.amischolar.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ResearchProject::class], version = 1, exportSchema = false)
abstract class ScholarDatabase : RoomDatabase() {
    abstract fun researchProjectDao(): ResearchProjectDao

    companion object {
        @Volatile private var instance: ScholarDatabase? = null

        fun getInstance(context: Context): ScholarDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScholarDatabase::class.java,
                    "am_i_scholar.db"
                ).build().also { instance = it }
            }
    }
}
