package com.example.yogaapp

import android.os.Bundle
import android.widget.*
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var dbHelper: YogaDatabaseHelper
    private lateinit var classListView: ListView
    private lateinit var instanceListView: ListView
    private lateinit var classAdapter: ArrayAdapter<String>
    private lateinit var instanceAdapter: ArrayAdapter<String>
    private var yogaClassList = mutableListOf<YogaClass>()
    private var classInstanceList = mutableListOf<ClassInstance>()
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var searchResultsListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = YogaDatabaseHelper(this)
        classListView = findViewById(R.id.classListView)
        instanceListView = findViewById(R.id.instanceListView)

        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        searchResultsListView = findViewById(R.id.searchResultsListView)

        searchButton.setOnClickListener {
            val searchTerm = searchInput.text.toString().trim()
            if (searchTerm.isNotEmpty()) {
                val searchResults = dbHelper.searchClassesByTeacher(searchTerm)
                displaySearchResults(searchResults)
            }
        }

        val dayInput: EditText = findViewById(R.id.dayInput)
        val timeInput: EditText = findViewById(R.id.timeInput)
        val capacityInput: EditText = findViewById(R.id.capacityInput)
        val durationInput: EditText = findViewById(R.id.durationInput)
        val priceInput: EditText = findViewById(R.id.priceInput)
        val typeInput: EditText = findViewById(R.id.typeInput)
        val descriptionInput: EditText = findViewById(R.id.descriptionInput)

        val dateInput: EditText = findViewById(R.id.dateInput)
        val teacherInput: EditText = findViewById(R.id.teacherInput)
        val commentsInput: EditText = findViewById(R.id.commentsInput)
        val instructorInput: EditText = findViewById(R.id.instructorInput)

        loadYogaClasses()

        // Button to add a new yoga class
        val addButton: Button = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            // Get data from input fields
            val day = dayInput.text.toString()
            val time = timeInput.text.toString()
            val capacity = capacityInput.text.toString().toIntOrNull() ?: 0
            val duration = durationInput.text.toString().toIntOrNull() ?: 0
            val price = priceInput.text.toString().toDoubleOrNull() ?: 0.0
            val type = typeInput.text.toString()
            val description = descriptionInput.text.toString()
            val teacher = teacherInput.text.toString()
            val instructor = instructorInput.text.toString()

            // Add the yoga class to the database
            dbHelper.addYogaClass(day, time, capacity, duration, price, type, description, teacher)
            loadYogaClasses()
        }

        // Button to reset the database
        val resetButton: Button = findViewById(R.id.resetButton)
        resetButton.setOnClickListener {
            dbHelper.resetDatabase()
            loadYogaClasses()
        }

        classListView.setOnItemClickListener { _, _, position, _ ->
            val selectedClass = yogaClassList[position]
            loadClassInstances(selectedClass.id)
        }

        // Button to add a class instance
        val addInstanceButton: Button = findViewById(R.id.addInstanceButton)
        addInstanceButton.setOnClickListener {
            // Get data from input fields
            val date = dateInput.text.toString()
            val teacher = teacherInput.text.toString()
            val comments = commentsInput.text.toString()
            val selectedClass = yogaClassList.firstOrNull() ?: return@setOnClickListener

            // Add the class instance to the database
            dbHelper.addClassInstance(date, teacher, comments, selectedClass.id)
            loadClassInstances(selectedClass.id)
        }

        val dayField = findViewById<EditText>(R.id.dayInput)
        val timeField = findViewById<EditText>(R.id.timeInput)
        val capacityField = findViewById<EditText>(R.id.capacityInput)
        val durationField = findViewById<EditText>(R.id.durationInput)
        val priceField = findViewById<EditText>(R.id.priceInput)
        val typeField = findViewById<EditText>(R.id.typeInput)
        val descriptionField = findViewById<EditText>(R.id.descriptionInput)
        val submitButton = findViewById<Button>(R.id.submitButton)

        submitButton.setOnClickListener {
            val day = dayField.text.toString()
            val time = timeField.text.toString()
            val capacity = capacityField.text.toString()
            val duration = durationField.text.toString()
            val price = priceField.text.toString()
            val type = typeField.text.toString()
            val description = descriptionField.text.toString()

            // Validate required fields
            if (day.isBlank() || time.isBlank() || capacity.isBlank() ||
                duration.isBlank() || price.isBlank() || type.isBlank()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show confirmation dialog
            val details = """
                Day: $day
                Time: $time
                Capacity: $capacity
                Duration: $duration minutes
                Price: £$price
                Type: $type
                Description: ${if (description.isBlank()) "None" else description}
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Confirm Details")
                .setMessage(details)
                .setPositiveButton("Confirm") { _, _ ->
                    Toast.makeText(this, "Details confirmed!", Toast.LENGTH_SHORT).show()
                    // Here you can handle the saving logic
                }
                .setNegativeButton("Edit") { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
        }
    }

    private fun loadYogaClasses() {
        yogaClassList = dbHelper.getAllYogaClasses().toMutableList()
        val classNames = yogaClassList.map { "${it.day} - ${it.time} - ${it.type}" }
        classAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, classNames)
        classListView.adapter = classAdapter
    }

    private fun loadClassInstances(classId: Int) {
        classInstanceList = dbHelper.getClassInstances(classId).toMutableList()
        val instanceDetails = classInstanceList.map { "${it.date} - ${it.teacher}" }
        instanceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, instanceDetails)
        instanceListView.adapter = instanceAdapter
    }

    private fun displaySearchResults(searchResults: List<YogaClass>) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            searchResults.map { "${it.type} with ${it.teacher}" }
        )
        searchResultsListView.adapter = adapter

        searchResultsListView.setOnItemClickListener { _, _, position, _ ->
            val selectedClass = searchResults[position]
            showClassDetails(selectedClass)
        }
    }

    private fun showClassDetails(yogaClass: YogaClass) {
        AlertDialog.Builder(this)
            .setTitle("Class Details")
            .setMessage(
                "Day: ${yogaClass.day}\n" +
                        "Time: ${yogaClass.time}\n" +
                        "Capacity: ${yogaClass.capacity}\n" +
                        "Duration: ${yogaClass.duration}\n" +
                        "Price: £${yogaClass.price}\n" +
                        "Type: ${yogaClass.type}\n" +
                        "Description: ${yogaClass.description}"
            )
            .setPositiveButton("OK", null)
            .show()
    }
}
