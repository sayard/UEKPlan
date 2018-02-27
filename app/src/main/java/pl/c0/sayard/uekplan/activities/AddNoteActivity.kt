package pl.c0.sayard.uekplan.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import pl.c0.sayard.uekplan.R
import pl.c0.sayard.uekplan.db.ScheduleContract
import pl.c0.sayard.uekplan.db.ScheduleDbHelper
import java.text.SimpleDateFormat
import java.util.*

class AddNoteActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener{

    private val dateCalendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)
        title = getString(R.string.new_note)

        val intent = getIntent()
        val id = intent.getIntExtra(getString(R.string.note_id_extra), 0)

        val noteDate = findViewById<TextView>(R.id.note_date)
        noteDate.text = dateFormat.format(dateCalendar.time)
        noteDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                    this@AddNoteActivity,
                    this@AddNoteActivity,
                    dateCalendar.get(Calendar.YEAR),
                    dateCalendar.get(Calendar.MONTH),
                    dateCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.setTitle(getString(R.string.note_date))
            datePicker.show()
        }

        val noteHour = findViewById<TextView>(R.id.note_hour)
        noteHour.text = String.format("%02d:%02d", dateCalendar.get(Calendar.HOUR_OF_DAY), dateCalendar.get(Calendar.MINUTE))
        noteHour.setOnClickListener {
            val currentTime = Calendar.getInstance()
            val hour = currentTime.get(Calendar.HOUR_OF_DAY)
            val minute = currentTime.get(Calendar.MINUTE)
            val timePicker = TimePickerDialog(
                    this@AddNoteActivity,
                    TimePickerDialog.OnTimeSetListener
                    { _, selectedHour, selectedMinute -> noteHour.text = String.format("%02d:%02d", selectedHour, selectedMinute) },
                    hour,
                    minute,
                    true
            )
            timePicker.setTitle(getString(R.string.note_hour))
            timePicker.show()
        }

        val noteTitleET = findViewById<EditText>(R.id.note_title)
        val noteContent = findViewById<EditText>(R.id.note_content)
        if(id != 0){
            val dbHelper = ScheduleDbHelper(this)
            val db = dbHelper.readableDatabase
            val cursor = db.query(
                    ScheduleContract.NotesEntry.TABLE_NAME,
                    null,
                    "${ScheduleContract.NotesEntry._ID} = $id",
                    null,
                    null,
                    null,
                    null
            )
            cursor.moveToFirst()
            val title = cursor.getString(cursor.getColumnIndex(ScheduleContract.NotesEntry.TITLE))
            val content = cursor.getString(cursor.getColumnIndex(ScheduleContract.NotesEntry.CONTENT))
            val date = cursor.getString(cursor.getColumnIndex(ScheduleContract.NotesEntry.DATE))
            val hour = cursor.getString(cursor.getColumnIndex(ScheduleContract.NotesEntry.HOUR))

            noteTitleET.setText(title, TextView.BufferType.EDITABLE)
            noteContent.setText(content, TextView.BufferType.EDITABLE)
            noteDate.text = date
            noteDate.text = date

            cursor.close()
        }

        val saveNoteButton = findViewById<Button>(R.id.save_note_button)
        saveNoteButton.setOnClickListener {
            val dbHelper = ScheduleDbHelper(this@AddNoteActivity)
            val db = dbHelper.readableDatabase
            val contentValues = ContentValues()
            contentValues.put(ScheduleContract.NotesEntry.TITLE, noteTitleET.text.toString())
            contentValues.put(ScheduleContract.NotesEntry.CONTENT, noteContent.text.toString())
            contentValues.put(ScheduleContract.NotesEntry.DATE, noteDate.text as String?)
            contentValues.put(ScheduleContract.NotesEntry.HOUR, noteHour.text as String?)
            if(id == 0){
                db.insert(ScheduleContract.NotesEntry.TABLE_NAME, null, contentValues)
            }else{
                db.update(ScheduleContract.NotesEntry.TABLE_NAME, contentValues, "${ScheduleContract.NotesEntry._ID} = $id", null)
            }
            finish()
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        dateCalendar.set(Calendar.YEAR, year)
        dateCalendar.set(Calendar.MONTH, monthOfYear)
        dateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        val noteDate = findViewById<TextView>(R.id.note_date)
        noteDate.text = dateFormat.format(dateCalendar.time)
    }
}