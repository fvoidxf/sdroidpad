package com.secnote.pad

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NoteEditActivity : AppCompatActivity() {

    private lateinit var titleEdit: EditText
    private lateinit var contentEdit: EditText
    private lateinit var algoSpinner: Spinner
    private lateinit var notesManager: NotesManager

    private var noteId: String? = null
    private var originalAlgorithm: String = CryptoManager.ALGO_AES256

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_edit)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        notesManager = NotesManager(this)
        titleEdit = findViewById(R.id.editTitle)
        contentEdit = findViewById(R.id.editContent)
        algoSpinner = findViewById(R.id.spinnerAlgorithm)

        val algorithms = arrayOf(CryptoManager.ALGO_AES256, CryptoManager.ALGO_KUZNECHIK, CryptoManager.ALGO_MAGMA)
        algoSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, algorithms)

        noteId = intent.getStringExtra("note_id")
        if (noteId != null) {
            titleEdit.setText(intent.getStringExtra("note_title") ?: "")
            contentEdit.setText(intent.getStringExtra("note_content") ?: "")
            originalAlgorithm = intent.getStringExtra("note_algorithm") ?: CryptoManager.ALGO_AES256
            val pos = algorithms.indexOf(originalAlgorithm)
            if (pos >= 0) algoSpinner.setSelection(pos)
            supportActionBar?.title = "Редактирование"
        } else {
            supportActionBar?.title = "Новая запись"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Сохранить").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            1 -> { promptPasswordAndSave(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun promptPasswordAndSave() {
        val title = titleEdit.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Введите заголовок", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = PasswordDialogFragment.newInstance("Пароль для сохранения") { password ->
            if (password.length < 4) {
                Toast.makeText(this, "Пароль слишком короткий", Toast.LENGTH_SHORT).show()
                return@newInstance
            }
            val content = contentEdit.text.toString()
            val algorithm = algoSpinner.selectedItem as String
            try {
                notesManager.saveNote(noteId, title, content, password, algorithm)
                Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка шифрования: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        dialog.show(supportFragmentManager, "save_password")
    }
}
