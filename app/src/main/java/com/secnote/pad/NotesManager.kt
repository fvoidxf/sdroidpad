package com.secnote.pad

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class NotesManager(private val context: Context) {

    private val notesDir: File
        get() = File(context.filesDir, "notes").also { it.mkdirs() }

    private val indexFile: File
        get() = File(context.filesDir, "notes_index.json")

    fun listNotes(): List<NoteMeta> {
        if (!indexFile.exists()) return emptyList()
        val text = indexFile.readText()
        if (text.isBlank()) return emptyList()
        val arr = JSONArray(text)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            NoteMeta(
                id = obj.getString("id"),
                title = obj.getString("title"),
                algorithm = obj.getString("algorithm"),
                createdAt = obj.getLong("createdAt"),
                modifiedAt = obj.getLong("modifiedAt")
            )
        }
    }

    fun saveNote(id: String?, title: String, content: String, password: String, algorithm: String): NoteMeta {
        val noteId = id ?: UUID.randomUUID().toString()
        val encrypted = CryptoManager.encrypt(content.toByteArray(Charsets.UTF_8), password, algorithm)
        val noteFile = File(notesDir, "$noteId.enc")
        noteFile.writeBytes(encrypted)

        val now = System.currentTimeMillis()
        val notes = listNotes().toMutableList()
        val existing = notes.indexOfFirst { it.id == noteId }
        val meta = if (existing >= 0) {
            val old = notes[existing]
            notes[existing] = old.copy(title = title, algorithm = algorithm, modifiedAt = now)
            notes[existing]
        } else {
            val new = NoteMeta(noteId, title, algorithm, now, now)
            notes.add(new)
            new
        }
        saveIndex(notes)
        return meta
    }

    fun readNote(id: String, password: String): String? {
        val noteFile = File(notesDir, "$id.enc")
        if (!noteFile.exists()) return null
        val notes = listNotes()
        val meta = notes.find { it.id == id } ?: return null
        return try {
            val decrypted = CryptoManager.decrypt(noteFile.readBytes(), password, meta.algorithm)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteNote(id: String) {
        val noteFile = File(notesDir, "$id.enc")
        if (noteFile.exists()) {
            val data = noteFile.readBytes()
            val overwritten = CryptoManager.secureDelete(data)
            noteFile.writeBytes(overwritten)
            noteFile.delete()
        }
        val notes = listNotes().filter { it.id != id }
        saveIndex(notes)
    }

    private fun saveIndex(notes: List<NoteMeta>) {
        val arr = JSONArray()
        for (note in notes) {
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("algorithm", note.algorithm)
            obj.put("createdAt", note.createdAt)
            obj.put("modifiedAt", note.modifiedAt)
            arr.put(obj)
        }
        indexFile.writeText(arr.toString())
    }
}
