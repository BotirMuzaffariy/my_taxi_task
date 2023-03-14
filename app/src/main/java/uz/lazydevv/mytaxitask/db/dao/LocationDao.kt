package uz.lazydevv.mytaxitask.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import uz.lazydevv.mytaxitask.db.entity.LocationEntity

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addLocation(location: LocationEntity)
}