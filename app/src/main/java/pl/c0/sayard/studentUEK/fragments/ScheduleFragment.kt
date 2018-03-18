package pl.c0.sayard.studentUEK.fragments

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import pl.c0.sayard.studentUEK.BackButtonEditText
import pl.c0.sayard.studentUEK.activities.AddLessonActivity
import pl.c0.sayard.studentUEK.R
import pl.c0.sayard.studentUEK.data.ScheduleItem
import pl.c0.sayard.studentUEK.Utils
import pl.c0.sayard.studentUEK.Utils.Companion.getLanguageGroups
import pl.c0.sayard.studentUEK.Utils.Companion.getScheduleCursor
import pl.c0.sayard.studentUEK.activities.ScheduleItemDetailsActivity
import pl.c0.sayard.studentUEK.adapters.ScheduleAdapter
import pl.c0.sayard.studentUEK.db.ScheduleDbHelper
import pl.c0.sayard.studentUEK.jobs.RefreshScheduleJob
import pl.c0.sayard.studentUEK.parsers.ScheduleParser


class ScheduleFragment : Fragment() {

    private var scheduleSearch: BackButtonEditText? = null

    companion object {
        fun newInstance(): ScheduleFragment{
            return ScheduleFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater!!.inflate(R.layout.fragment_schedule, container, false)

        val dbHelper = ScheduleDbHelper(activity)
        val db = dbHelper.readableDatabase
        val progressBar = view.findViewById<ProgressBar>(R.id.schedule_progress_bar)
        val errorMessage = view.findViewById<TextView>(R.id.schedule_error_message)
        val urls = mutableListOf<String>()
        val groups = Utils.getGroups(db)
        groups.mapTo(urls){it.url}
        val languageGroups = getLanguageGroups(db)
        languageGroups.mapTo(urls) { it.url }
        val cursor = getScheduleCursor(db)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if(prefs.getBoolean(getString(R.string.PREFS_REFRESH_SCHEDULE), false) || cursor.count == 0){
            ScheduleParser(context, activity, progressBar, errorMessage, null, null).execute(urls)
            prefs.edit().putBoolean(getString(R.string.PREFS_REFRESH_SCHEDULE), false).apply()
        }else{
            val scheduleList = Utils.getScheduleList(cursor, db)
            if (scheduleList.isNotEmpty()){
                errorMessage.visibility = View.GONE
                val adapter = getAdapter(scheduleList)
                scheduleSearch = view.findViewById<BackButtonEditText>(R.id.schedule_search)
                scheduleSearch?.addTextChangedListener(object: TextWatcher{
                    override fun afterTextChanged(p0: Editable?) {
                    }

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        adapter.filter.filter(p0.toString())
                    }
                })
                scheduleSearch?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if(!hasFocus && scheduleSearch?.text.toString() == ""){
                        scheduleSearch?.visibility = View.GONE
                    }
                }
                val listView = view.findViewById<ListView>(R.id.schedule_list_view)
                listView.adapter = adapter
                listView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val scheduleItem = parent.getItemAtPosition(position) as ScheduleItem
                    val intent = Intent(context, ScheduleItemDetailsActivity::class.java).apply {
                        putExtra(getString(R.string.subject_extra), scheduleItem.subject)
                        putExtra(getString(R.string.type_extra), scheduleItem.type)
                        putExtra(getString(R.string.teacher_extra), scheduleItem.teacher)
                        putExtra(getString(R.string.teacher_id_extra), scheduleItem.teacherId)
                        putExtra(getString(R.string.classroom_extra), scheduleItem.classroom)
                        putExtra(getString(R.string.comments_extra), scheduleItem.comments)
                        putExtra(getString(R.string.date_extra), scheduleItem.dateStr)
                        putExtra(getString(R.string.start_date_extra), scheduleItem.startDateStr)
                        putExtra(getString(R.string.end_date_extra), scheduleItem.endDateStr)
                        putExtra(getString(R.string.is_custom_extra), scheduleItem.isCustom)
                        putExtra(getString(R.string.extra_custom_id), scheduleItem.customId)
                        putExtra(getString(R.string.extra_note_id), scheduleItem.noteId)
                        putExtra(getString(R.string.extra_note_content), scheduleItem.noteContent)
                    }
                    startActivity(intent)
                }
                val scheduleSwipe = view.findViewById<SwipeRefreshLayout>(R.id.schedule_swipe)
                scheduleSwipe.setOnRefreshListener{
                    val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val networkInfo = connMgr.activeNetworkInfo
                    if(networkInfo != null && networkInfo.isConnected){
                        ScheduleParser(context, null, null, errorMessage, adapter, scheduleSwipe).execute(urls)
                        scheduleSearch?.setText("", TextView.BufferType.EDITABLE)
                        Toast.makeText(context, getString(R.string.schedule_refreshed), Toast.LENGTH_SHORT).show()
                        Thread{
                            kotlin.run {
                                RefreshScheduleJob.refreshSchedule(context)
                            }
                        }.start()
                    }else{
                        Toast.makeText(context, getString(R.string.no_internet_conn), Toast.LENGTH_SHORT).show()
                        scheduleSwipe.isRefreshing = false
                    }
                }
            }else{
                errorMessage.visibility = View.VISIBLE
                val scheduleSwipe = view.findViewById<SwipeRefreshLayout>(R.id.schedule_swipe)
                scheduleSwipe.setOnRefreshListener{
                    val ft = activity.supportFragmentManager.beginTransaction()
                    ft.detach(this)
                    ft.attach(this)
                    ft.commit()
                }
            }
        }
        cursor.close()
        setHasOptionsMenu(true)
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.schedule_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.new_schedule_item -> {
                val newLessonIntent = Intent(context, AddLessonActivity::class.java)
                startActivity(newLessonIntent)
            }
            R.id.search_schedule_item -> {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if(scheduleSearch?.visibility == View.GONE){
                    scheduleSearch?.visibility = View.VISIBLE
                    scheduleSearch?.isFocusableInTouchMode = true
                    scheduleSearch?.requestFocus()
                    imm.showSoftInput(scheduleSearch, InputMethodManager.SHOW_IMPLICIT)
                }else{
                    scheduleSearch?.visibility = View.GONE
                    imm.hideSoftInputFromWindow(scheduleSearch?.windowToken, 0)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getAdapter(scheduleList: List<ScheduleItem>): ScheduleAdapter{
        return ScheduleAdapter(context, scheduleList)
    }

    override fun onResume() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if(prefs.getBoolean(getString(R.string.PREFS_REFRESH_SCHEDULE), false)){
            val ft = activity.supportFragmentManager.beginTransaction()
            ft.detach(this).attach(this).commit()
        }
        super.onResume()
    }

}
