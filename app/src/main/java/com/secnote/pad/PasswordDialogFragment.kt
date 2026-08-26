package com.secnote.pad

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class PasswordDialogFragment : DialogFragment() {

    private var onPasswordEntered: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        onPasswordEntered = arguments?.getSerializable("callback") as? (String) -> Unit
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString("title") ?: "Введите пароль"
        val input = EditText(requireContext()).apply {
            hint = "Пароль"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            val dp16 = (16 * resources.displayMetrics.density).toInt()
            setPadding(dp16, dp16, dp16, dp16)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val password = input.text.toString()
                if (password.isNotEmpty()) {
                    onPasswordEntered?.invoke(password)
                } else {
                    Toast.makeText(context, "Пароль не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()
    }

    companion object {
        fun newInstance(title: String, callback: (String) -> Unit): PasswordDialogFragment {
            return PasswordDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("title", title)
                    putSerializable("callback", callback as java.io.Serializable)
                }
            }
        }
    }
}
