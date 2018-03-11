package pl.c0.sayard.studentUEK.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*

import pl.c0.sayard.studentUEK.R
import pl.c0.sayard.studentUEK.activities.AddNoteActivity
import pl.c0.sayard.studentUEK.adapters.NotesAdapter
import pl.c0.sayard.studentUEK.data.Note
import pl.c0.sayard.studentUEK.parsers.NotesParser


class NotesFragment : Fragment() {

    companion object {
        fun newInstance(): NotesFragment{
            return NotesFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater!!.inflate(R.layout.fragment_notes, container, false)
        val notesMessage = view.findViewById<TextView>(R.id.notes_message)
        val listView = view.findViewById<ListView>(R.id.notes_list_view)
        val notesSearch = view.findViewById<EditText>(R.id.notes_search)
        val progressBar = view.findViewById<ProgressBar>(R.id.notes_progress_bar)
        progressBar.visibility = View.VISIBLE

        executeNotesParser(progressBar, notesMessage, listView, notesSearch)

        setHasOptionsMenu(true)
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.notes_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.new_note_item -> {
                val newNoteIntent = Intent(context, AddNoteActivity::class.java)
                startActivity(newNoteIntent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if(view != null){
            val progressBar = view!!.findViewById<ProgressBar>(R.id.notes_progress_bar)
            val listView = view!!.findViewById<ListView>(R.id.notes_list_view)
            val notesMessage = view!!.findViewById<TextView>(R.id.notes_message)
            val notesSearch = view!!.findViewById<EditText>(R.id.notes_search)
            notesSearch.setText("", TextView.BufferType.EDITABLE)
            executeNotesParser(progressBar, notesMessage, listView, notesSearch)
        }
    }

    private fun executeNotesParser(progressBar: ProgressBar, notesMessage: TextView, listView: ListView, notesSearch:EditText){
        NotesParser(this, object: NotesParser.OnTaskCompleted{

            override fun onTaskCompleted(result: List<Note>?) {
                if(context != null){
                    progressBar.visibility = View.GONE
                    if(result == null){
                        notesMessage.visibility = View.VISIBLE
                    }else{
                        val adapter = NotesAdapter(context, result.toMutableList())
                        notesSearch.addTextChangedListener(object: TextWatcher{
                            override fun afterTextChanged(p0: Editable?) {}

                            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                                adapter.filter.filter(p0.toString())
                            }

                        })
                        listView.adapter = adapter
                        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                            val note = parent.getItemAtPosition(position) as Note
                            val intent = Intent(context, AddNoteActivity::class.java)
                            intent.putExtra(getString(R.string.note_id_extra), note.id)
                            startActivity(intent)
                        }
                    }
                }
            }

        }).execute()
    }

}
