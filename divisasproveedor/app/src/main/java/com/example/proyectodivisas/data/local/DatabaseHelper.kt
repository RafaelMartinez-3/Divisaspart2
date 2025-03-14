package com.example.proyectodivisas.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "exchange_rates.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "exchange_rates"

        const val COLUMN_ID = "id"
        const val COLUMN_CURRENCY = "currency"
        const val COLUMN_RATE = "rate"
        const val COLUMN_DATE = "date"

        private const val SQL_CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CURRENCY TEXT NOT NULL,
                $COLUMN_RATE REAL NOT NULL,
                $COLUMN_DATE TEXT NOT NULL
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}
