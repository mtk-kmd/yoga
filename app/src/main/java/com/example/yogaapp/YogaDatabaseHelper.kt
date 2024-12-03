package com.example.yogaapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStream
import org.json.JSONObject

class YogaDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "yogaClasses.db"
        private const val DATABASE_VERSION = 2
        const val TABLE_CLASSES = "YogaClass"
        const val TABLE_INSTANCES = "ClassInstance"
        const val COLUMN_ID = "id"
        const val TABLE_NAME = "YogaClass"
        const val COLUMN_DAY = "day"
        const val COLUMN_TIME = "time"
        const val COLUMN_CAPACITY = "capacity"
        const val COLUMN_DURATION = "duration"
        const val COLUMN_PRICE = "price"
        const val COLUMN_TYPE = "type"
        const val COLUMN_DESCRIPTION = "description"

        // Class Instance columns
        const val COLUMN_DATE = "date"
        const val COLUMN_TEACHER = "teacher"
        const val COLUMN_COMMENTS = "comments"
        const val COLUMN_CLASS_ID = "class_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createClassesTable = ("CREATE TABLE $TABLE_CLASSES (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_DAY TEXT NOT NULL, " +
                "$COLUMN_TIME TEXT NOT NULL, " +
                "$COLUMN_CAPACITY INTEGER NOT NULL, " +
                "$COLUMN_DURATION INTEGER NOT NULL, " +
                "$COLUMN_PRICE REAL NOT NULL, " +
                "$COLUMN_TYPE TEXT NOT NULL, " +
                "$COLUMN_DESCRIPTION TEXT, " +
                "$COLUMN_TEACHER TEXT" +
                ")")

        val createInstancesTable = ("CREATE TABLE $TABLE_INSTANCES (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_DATE TEXT NOT NULL, " +
                "$COLUMN_TEACHER TEXT NOT NULL, " +
                "$COLUMN_COMMENTS TEXT, " +
                "$COLUMN_CLASS_ID INTEGER, " +
                "FOREIGN KEY($COLUMN_CLASS_ID) REFERENCES $TABLE_CLASSES($COLUMN_ID))")

        db.execSQL(createClassesTable)
        db.execSQL(createInstancesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CLASSES")
        onCreate(db)
    }

    fun addYogaClass(day: String, time: String, capacity: Int, duration: Int, price: Double, type: String, description: String?, teacher: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://yoga.mtktechlab.com/classes")
                val urlConnection = url.openConnection() as HttpURLConnection

                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.doOutput = true

                val descriptionValue = description ?: ""
                val teacherValue = teacher ?: ""

                val jsonObject = JSONObject().apply {
                    put("day", day)
                    put("time", time)
                    put("capacity", capacity)
                    put("duration", duration)
                    put("price", price)
                    put("type", type)
                    put("description", descriptionValue)
                    put("teacher", teacherValue)
                }

                withContext(Dispatchers.IO) {
                    urlConnection.outputStream.use { outputStream: OutputStream ->
                        outputStream.write(jsonObject.toString().toByteArray())
                        outputStream.flush()
                    }
                }

                val responseCode = urlConnection.responseCode
                withContext(Dispatchers.Main) {
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        println("Class added successfully")
                    } else {
                        println("Failed to add class. Response code: $responseCode")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addClassInstance(date: String, teacher: String, comments: String?, classId: Int) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_DATE, date)
        values.put(COLUMN_TEACHER, teacher)
        values.put(COLUMN_COMMENTS, comments)
        values.put(COLUMN_CLASS_ID, classId)

        db.insert(TABLE_INSTANCES, null, values)
        db.close()
    }

    fun getClassInstances(classId: Int): List<ClassInstance> {
        val classInstances = mutableListOf<ClassInstance>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_INSTANCES WHERE $COLUMN_CLASS_ID = ?", arrayOf(classId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val instance = ClassInstance(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEACHER)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENTS)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLASS_ID))
                )
                classInstances.add(instance)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return classInstances
    }

    fun deleteClassInstance(instanceId: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_INSTANCES, "$COLUMN_ID=?", arrayOf(instanceId.toString()))
        db.close()
    }

     suspend fun getAllYogaClasses(): List<YogaClass> {
        val yogaClasses = mutableListOf<YogaClass>()
        val url = URL("https://yoga.mtktechlab.com/classes")

        withContext(Dispatchers.IO) {
            val urlConnection = url.openConnection() as HttpURLConnection
            try {
                urlConnection.requestMethod = "GET"
                urlConnection.setRequestProperty("Content-Type", "application/json")
                val responseCode = urlConnection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = urlConnection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }

                    // Parse JSON response
                    val jsonArray = JSONArray(response)
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val yogaClass = YogaClass(
                            jsonObject.getInt("id"),
                            jsonObject.getString("day"),
                            jsonObject.getString("time"),
                            jsonObject.getInt("capacity"),
                            jsonObject.getInt("duration"),
                            jsonObject.getDouble("price"),
                            jsonObject.getString("type"),
                            jsonObject.optString("description", ""),
                            jsonObject.optString("teacher", "")
                        )
                        yogaClasses.add(yogaClass)
                    }
                } else {
                    Log.e("API Error", "Failed to fetch classes. Response code: $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                urlConnection.disconnect()
            }
        }
        return yogaClasses
    }

    fun deleteYogaClass(id: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
    }

    fun resetDatabase() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM $TABLE_NAME")
        db.close()
    }

    fun searchClassesByDay(day: String): List<YogaClass> {
        val yogaClasses = mutableListOf<YogaClass>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CLASSES WHERE $COLUMN_DAY = ?",
            arrayOf(day)
        )

        if (cursor.moveToFirst()) {
            do {
                val yogaClass = YogaClass(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAPACITY)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DURATION)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEACHER))
                )
                yogaClasses.add(yogaClass)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return yogaClasses
    }
}
