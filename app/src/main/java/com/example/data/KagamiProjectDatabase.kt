package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "kagami_projects")
data class KagamiProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val thumbnailPath: String,
    val baseLayerPath: String?,
    val drawingLayerPath: String,
    val strokesJson: String,
    val filterMode: String,
    val filterIntensity: Float,
    val referenceAlpha: Float,
    val canvasBackgroundHex: Long,
    val width: Int,
    val height: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface KagamiProjectDao {
    @Query("SELECT * FROM kagami_projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<KagamiProject>>

    @Query("SELECT * FROM kagami_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): KagamiProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: KagamiProject): Long

    @Update
    suspend fun updateProject(project: KagamiProject)

    @Query("DELETE FROM kagami_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("DELETE FROM kagami_projects")
    suspend fun deleteAllProjects()
}

@Database(entities = [KagamiProject::class], version = 1, exportSchema = false)
abstract class KagamiDatabase : RoomDatabase() {
    abstract fun projectDao(): KagamiProjectDao

    companion object {
        @Volatile
        private var INSTANCE: KagamiDatabase? = null

        fun getDatabase(context: Context): KagamiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KagamiDatabase::class.java,
                    "kagami_atelier.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
