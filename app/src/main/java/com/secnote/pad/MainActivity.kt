package com.secnote.pad

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var notesManager: NotesManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notesManager = NotesManager(this)
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)

        adapter = NotesAdapter(
            onClick = { meta -> showPasswordDialog(meta, isDelete = false) },
            onLongClick = { meta, view -> showContextMenu(meta, view) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, NoteEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val notes = notesManager.listNotes().sortedByDescending { it.modifiedAt }
        adapter.submit(notes)
        emptyView.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (notes.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showPasswordDialog(meta: NoteMeta, isDelete: Boolean) {
        val dialog = PasswordDialogFragment.newInstance(meta.title) { password ->
            if (isDelete) {
                val content = notesManager.readNote(meta.id, password)
                if (content != null) {
                    notesManager.deleteNote(meta.id)
                    refreshList()
                } else {
                    showError("Неверный пароль")
                }
            } else {
                val content = notesManager.readNote(meta.id, password)
                if (content != null) {
                    val intent = Intent(this, NoteEditActivity::class.java).apply {
                        putExtra("note_id", meta.id)
                        putExtra("note_title", meta.title)
                        putExtra("note_content", content)
                        putExtra("note_algorithm", meta.algorithm)
                    }
                    startActivity(intent)
                } else {
                    showError("Неверный пароль")
                }
            }
        }
        dialog.show(supportFragmentManager, "password")
    }

    private fun showContextMenu(meta: NoteMeta, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Удалить")
        popup.setOnMenuItemClickListener {
            showPasswordDialog(meta, isDelete = true)
            true
        }
        popup.show()
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    inner class NotesAdapter(
        private val onClick: (NoteMeta) -> Unit,
        private val onLongClick: (NoteMeta, View) -> Unit
    ) : RecyclerView.Adapter<NotesAdapter.VH>() {

        private var items = listOf<NoteMeta>()

        fun submit(list: List<NoteMeta>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_note, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.algorithm.text = item.algorithm
            holder.date.text = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(item.modifiedAt))
            holder.itemView.setOnClickListener { onClick(item) }
            holder.itemView.setOnLongClickListener { v -> onLongClick(item, v); true }
        }

        override fun getItemCount() = items.size

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.noteTitle)
            val algorithm: TextView = view.findViewById(R.id.noteAlgorithm)
            val date: TextView = view.findViewById(R.id.noteDate)
        }
    }
}
