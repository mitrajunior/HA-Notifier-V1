package com.example.hanotifier.data

import android.content.Context
import androidx.room.Room

object DbProvider {
  @Volatile private var db: AppDb? = null
  fun get(ctx: Context): AppDb {
    return db ?: synchronized(this) {
      db ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "ha_notifier.db")
        .fallbackToDestructiveMigration()
        .build()
        .also { db = it }
    }
  }
}
