package uz.lazydevv.mytaxitask.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import uz.lazydevv.mytaxitask.db.dao.LocationDao
import uz.lazydevv.mytaxitask.db.entity.LocationEntity

@Database(entities = [LocationEntity::class], version = 1)
abstract class AppDb : RoomDatabase() {

    abstract fun locationDao(): LocationDao

    companion object {
        private const val DB_NAME = "My taxi task"
        private lateinit var instance: AppDb

        @Synchronized
        fun getInstance(context: Context): AppDb {
            if (!::instance.isInitialized) {
                instance = Room.databaseBuilder(context, AppDb::class.java, DB_NAME).build()
            }

            return instance
        }
    }
}