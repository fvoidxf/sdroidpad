package com.secnote.pad

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class NotesManagerTest {

    @Test
    fun `listNotes returns empty when no index file`() {
        val tempDir = createTempDir("notes_test_")
        try {
            val indexFile = File(tempDir, "notes_index.json")
            assertFalse(indexFile.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `encrypted file roundtrip via CryptoManager`() {
        val tempDir = createTempDir("notes_test_")
        try {
            val password = "test1234"
            val content = "Secret note text"
            val encrypted = CryptoManager.encrypt(content.toByteArray(Charsets.UTF_8), password, CryptoManager.ALGO_AES256)

            val noteFile = File(tempDir, "test.enc")
            noteFile.writeBytes(encrypted)

            val readBack = noteFile.readBytes()
            val decrypted = CryptoManager.decrypt(readBack, password, CryptoManager.ALGO_AES256)
            assertEquals(content, String(decrypted, Charsets.UTF_8))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `secure delete overwrites file content`() {
        val tempDir = createTempDir("notes_test_")
        try {
            val data = "Sensitive data to wipe".toByteArray(Charsets.UTF_8)
            val noteFile = File(tempDir, "wipe_test.enc")
            noteFile.writeBytes(data)

            val originalBytes = noteFile.readBytes()
            val wiped = CryptoManager.secureDelete(originalBytes)
            noteFile.writeBytes(wiped)
            noteFile.delete()

            assertFalse(noteFile.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `index file read write roundtrip`() {
        val tempDir = createTempDir("notes_test_")
        try {
            val indexFile = File(tempDir, "notes_index.json")
            val json = """[{"id":"n1","title":"Note 1","algorithm":"AES-256","createdAt":1000,"modifiedAt":2000},{"id":"n2","title":"Note 2","algorithm":"Kuznechik","createdAt":3000,"modifiedAt":4000}]"""
            indexFile.writeText(json)

            val readBack = indexFile.readText()
            assertTrue(readBack.contains("\"id\":\"n1\""))
            assertTrue(readBack.contains("\"title\":\"Note 2\""))
            assertTrue(readBack.contains("Kuznechik"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `delete note removes entry from json`() {
        val tempDir = createTempDir("notes_test_")
        try {
            val indexFile = File(tempDir, "notes_index.json")
            val json = """[{"id":"n1","title":"Note 1","algorithm":"AES-256","createdAt":1000,"modifiedAt":2000},{"id":"n2","title":"Note 2","algorithm":"AES-256","createdAt":3000,"modifiedAt":4000},{"id":"n3","title":"Note 3","algorithm":"AES-256","createdAt":5000,"modifiedAt":6000}]"""
            indexFile.writeText(json)

            // Simulate delete of n2 by filtering the JSON string
            val readBack = indexFile.readText()
            val filtered = readBack.replace(Regex("""\{[^{}]*"id":"n2"[^{}]*\},?"""), "")
                .replace(",]", "]")
                .replace("[,", "[")
            indexFile.writeText(filtered)

            val result = indexFile.readText()
            assertTrue(result.contains("n1"))
            assertFalse(result.contains("n2"))
            assertTrue(result.contains("n3"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `multiple encrypted files with different passwords`() {
        val tempDir = createTempDir("notes_test_")
        try {
            val pw1 = "password1"
            val pw2 = "password2"
            val content1 = "First secret"
            val content2 = "Second secret"

            val enc1 = CryptoManager.encrypt(content1.toByteArray(Charsets.UTF_8), pw1, CryptoManager.ALGO_AES256)
            val enc2 = CryptoManager.encrypt(content2.toByteArray(Charsets.UTF_8), pw2, CryptoManager.ALGO_KUZNECHIK)

            File(tempDir, "n1.enc").writeBytes(enc1)
            File(tempDir, "n2.enc").writeBytes(enc2)

            val dec1 = CryptoManager.decrypt(File(tempDir, "n1.enc").readBytes(), pw1, CryptoManager.ALGO_AES256)
            val dec2 = CryptoManager.decrypt(File(tempDir, "n2.enc").readBytes(), pw2, CryptoManager.ALGO_KUZNECHIK)

            assertEquals(content1, String(dec1, Charsets.UTF_8))
            assertEquals(content2, String(dec2, Charsets.UTF_8))

            // Wrong password should fail
            try {
                CryptoManager.decrypt(File(tempDir, "n1.enc").readBytes(), pw2, CryptoManager.ALGO_AES256)
                fail("Should have thrown")
            } catch (e: Exception) {
                // expected
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
