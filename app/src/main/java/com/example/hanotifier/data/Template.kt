package com.example.hanotifier.data

import androidx.room.*

@Entity(tableName = "templates")
data class Template(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val priority: String = "info",      // info|warning|critical
  val persistent: Boolean = false,
  val popup: Boolean = false,
  val requireAck: Boolean = false
)

@Dao
interface TemplateDao {
  @Insert suspend fun insert(t: Template): Long
  @Update suspend fun update(t: Template)
  @Delete suspend fun delete(t: Template)
  @Query("SELECT * FROM templates ORDER BY name ASC") suspend fun all(): List<Template>
  @Query("SELECT * FROM templates WHERE id = :id") suspend fun get(id: Long): Template?
  @Query("SELECT * FROM templates WHERE name = :name LIMIT 1") suspend fun byName(name: String): Template?
}
