package com.example.yogaapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_DAY, day)
        values.put(COLUMN_TIME, time)
        values.put(COLUMN_CAPACITY, capacity)
        values.put(COLUMN_DURATION, duration)
        values.put(COLUMN_PRICE, price)
        values.put(COLUMN_TYPE, type)
        values.put(COLUMN_DESCRIPTION, description)
        values.put(COLUMN_TEACHER, teacher)

        db.insert(TABLE_NAME, null, values)
        db.close()
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

    fun getAllYogaClasses(): List<YogaClass> {
        val yogaClasses = mutableListOf<YogaClass>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

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

    fun searchClassesByTeacher(teacher: String): List<YogaClass> {
        val results = mutableListOf<YogaClass>()
        val db = this.readableDatabase
        val searchTerm = "%${teacher.trim()}%"

        val cursor = db.rawQuery(
            "SELECT id, day, time, capacity, duration, price, type, description, teacher FROM $TABLE_CLASSES WHERE teacher LIKE ?",
            arrayOf(searchTerm)
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val day = cursor.getString(cursor.getColumnIndexOrThrow("day"))
                val time = cursor.getString(cursor.getColumnIndexOrThrow("time"))
                val capacity = cursor.getInt(cursor.getColumnIndexOrThrow("capacity"))
                val duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration"))
                val price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"))
                val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                val teacherName = cursor.getString(cursor.getColumnIndexOrThrow("teacher"))

                results.add(YogaClass(id, day, time, capacity, duration, price, type, description, teacherName))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return results
    }

    fun searchClassesByDayOrDate(query: String): List<YogaClass> {
        val db = this.readableDatabase
        val classList = mutableListOf<YogaClass>()
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CLASSES WHERE day = ? OR date = ?",
            arrayOf(query, query)
        )
        if (cursor.moveToFirst()) {
            do {
                val yogaClass = YogaClass(
                    id = cursor.getInt(cursor.getColumnIndex("id")),
                    day = cursor.getString(cursor.getColumnIndex("day")),
                    time = cursor.getString(cursor.getColumnIndex("time")),
                    capacity = cursor.getInt(cursor.getColumnIndex("capacity")),
                    duration = cursor.getInt(cursor.getColumnIndex("duration")),
                    price = cursor.getDouble(cursor.getColumnIndex("price")),
                    type = cursor.getString(cursor.getColumnIndex("type")),
                    description = cursor.getString(cursor.getColumnIndex("description")),
                    teacher = cursor.getString(cursor.getColumnIndex("teacher"))
                )
                classList.add(yogaClass)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return classList
    }
}
