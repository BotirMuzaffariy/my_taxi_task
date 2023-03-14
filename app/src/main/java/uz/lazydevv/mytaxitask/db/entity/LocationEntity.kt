package uz.lazydevv.mytaxitask.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val latitude: Double,
    val longitude: Double
)