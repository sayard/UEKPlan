package pl.c0.sayard.studentUEK.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import pl.c0.sayard.studentUEK.R
import pl.c0.sayard.studentUEK.Utils
import pl.c0.sayard.studentUEK.adapters.ScheduleAdapter
import pl.c0.sayard.studentUEK.data.Group
import pl.c0.sayard.studentUEK.data.ScheduleItem
import pl.c0.sayard.studentUEK.parsers.SearchedScheduleParser

class SearchedScheduleActivity : AppCompatActivity() {

    private var groupType: String? = null
    private var teacherId = 0
    private var isLongSchedule = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.onActivityCreateSetTheme(this)
        setContentView(R.layout.activity_searched_schedule)
        this.groupType = intent.getStringExtra(getString(R.string.EXTRA_SEARCHED_SCHEDULE_GROUP_TYPE))
        val groupName = intent.getStringExtra(getString(R.string.EXTRA_SEARCHED_SCHEDULE_GROUP_NAME))
        title = groupName
        if(groupType != null){
            val group = Group(
                    intent.getIntExtra(getString(R.string.EXTRA_SEARCHED_SCHEDULE_GROUP_ID), 0),
                    groupName,
                    groupType!!
            )
            val progressBar = findViewById<ProgressBar>(R.id.searched_schedule_progress_bar)
            val errorMessage= findViewById<TextView>(R.id.searched_schedule_error_message)
            executeSearchedScheduleParser(progressBar, errorMessage, group, isLongSchedule)
            val searchedScheduleSwipe = findViewById<SwipeRefreshLayout>(R.id.searched_schedule_swipe)
            searchedScheduleSwipe.setOnRefreshListener {
                executeSearchedScheduleParser(progressBar, errorMessage, group, isLongSchedule)
                Toast.makeText(this, getString(R.string.schedule_refreshed), Toast.LENGTH_SHORT).show()
                searchedScheduleSwipe.isRefreshing = false
            }
        }
    }

    private fun executeSearchedScheduleParser(progressBar: ProgressBar, errorMessage: TextView, group: Group, isLongSchedule: Boolean){
        SearchedScheduleParser(progressBar, errorMessage, isLongSchedule, object: SearchedScheduleParser.OnTaskCompleted{
            override fun onTaskCompleted(result: List<ScheduleItem>) {
                for(i in 0 until result.size){
                    val scheduleItem = result[i]
                    if(i==0){
                        scheduleItem.isFirstOnTheDay = true
                        teacherId = result[i].teacherId
                    }else{
                        val previousScheduleItem = result[i-1]
                        if(scheduleItem.dateStr != previousScheduleItem.dateStr){
                            scheduleItem.isFirstOnTheDay = true
                        }
                    }
                }
                val adapter = getAdapter(result)
                if(adapter.count <= 0){
                    Toast.makeText(this@SearchedScheduleActivity, getText(R.string.error_try_again_later), Toast.LENGTH_SHORT).show()
                    errorMessage.visibility = View.VISIBLE
                }else{
                    errorMessage.visibility = View.GONE
                }

                val listView = findViewById<ListView>(R.id.searched_schedule_list_view)
                listView.adapter = adapter
            }

        }).execute(group)
    }

    private fun getAdapter(scheduleList: List<ScheduleItem>): ScheduleAdapter {
        return ScheduleAdapter(this, this, null, scheduleList)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.searched_schedule_menu, menu)
        if(groupType == "G"){
            menu?.findItem(R.id.teacher_page)?.isVisible = false
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.teacher_page -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, "https://e-uczelnia.uek.krakow.pl/course/view.php?id=$teacherId".toUri())
                startActivity(browserIntent)
            }
            R.id.schedule_length -> {
                val groupName = intent.getStringExtra(getString(R.string.EXTRA_SEARCHED_SCHEDULE_GROUP_NAME))
                val group = Group(
                        intent.getIntExtra(getString(R.string.EXTRA_SEARCHED_SCHEDULE_GROUP_ID), 0),
                        groupName,
                        groupType!!
                )
                val progressBar = findViewById<ProgressBar>(R.id.searched_schedule_progress_bar)
                val errorMessage= findViewById<TextView>(R.id.searched_schedule_error_message)
                isLongSchedule = !isLongSchedule
                if(isLongSchedule){
                    item.icon = getDrawable(R.drawable.ic_short_schedule_24dp)
                }else{
                    item.icon = getDrawable(R.drawable.ic_long_schedule_24dp)
                }
                executeSearchedScheduleParser(progressBar, errorMessage, group, isLongSchedule)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
