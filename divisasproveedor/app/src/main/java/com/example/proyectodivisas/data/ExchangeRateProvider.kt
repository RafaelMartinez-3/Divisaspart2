package com.example.proyectodivisas.data

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import com.example.proyectodivisas.data.local.DatabaseHelper

class ExchangeRateProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "com.example.proyectodivisas.provider"
        private const val TABLE_NAME = "exchange_rates"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$TABLE_NAME/")

        private const val CODE_RATES = 1
        private const val CODE_RATES_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, TABLE_NAME, CODE_RATES)
            addURI(AUTHORITY, "$TABLE_NAME/#", CODE_RATES_ID)
        }
    }

    private lateinit var database: SQLiteDatabase

    override fun onCreate(): Boolean {
        context?.let {
            database = DatabaseHelper(it).writableDatabase
            return true
        }
        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val queryBuilder = SQLiteQueryBuilder().apply {
            tables = TABLE_NAME
        }

        return when (uriMatcher.match(uri)) {
            CODE_RATES -> queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder)
            CODE_RATES_ID -> {
                val id = uri.lastPathSegment
                queryBuilder.appendWhere("id = $id")
                queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }?.apply {
            setNotificationUri(context?.contentResolver, uri)
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val id = database.insert(TABLE_NAME, null, values)
        return if (id > 0) {
            context?.contentResolver?.notifyChange(uri, null)
            ContentUris.withAppendedId(CONTENT_URI, id)
        } else {
            throw SQLException("Failed to insert row into $uri")
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        val count = database.update(TABLE_NAME, values, selection, selectionArgs)
        context?.contentResolver?.notifyChange(uri, null)
        return count
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val count = database.delete(TABLE_NAME, selection, selectionArgs)
        context?.contentResolver?.notifyChange(uri, null)
        return count
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_RATES -> "vnd.android.cursor.dir/$AUTHORITY.$TABLE_NAME"
            CODE_RATES_ID -> "vnd.android.cursor.item/$AUTHORITY.$TABLE_NAME"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}
