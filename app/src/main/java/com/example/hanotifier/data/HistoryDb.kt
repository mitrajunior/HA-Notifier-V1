package com.example.hanotifier.data

import androidx.room.*

@Entity(tableName = "history")
data class History(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val ts: Long,
  val title: String,
  val body: String,
  val priority: String
)

@Dao
interface HistoryDao {
  @Insert suspend fun insert(h: History)
  @Query("SELECT * FROM history ORDER BY ts DESC LIMIT :limit")
  suspend fun latest(limit: Int = 100): List<History>
}

@Database(
  entities = [History::class, Template::class],
  version = 2,
  exportSchema = false   // ✅ evita o warning no CI
)
abstract class AppDb: RoomDatabase() {
  abstract fun history(): HistoryDao
  abstract fun templates(): TemplateDao
}
