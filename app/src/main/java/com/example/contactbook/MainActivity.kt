package com.example.contactbook

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.contactbook.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ArrayAdapter<String>

    private val contacts = arrayListOf(
        "Ahmed Ben Ali", "Amel Trabelsi", "Karim Jlassi",
        "Mariem Gharbi", "Sami Chaabane", "Yasmine Hamdi"
    )
    private var selectedPos = AdapterView.INVALID_POSITION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Restauration après rotation
        savedInstanceState?.let {
            it.getStringArrayList(KEY_CONTACTS)?.let { saved ->
                contacts.clear()
                contacts.addAll(saved)
            }
            selectedPos = it.getInt(KEY_SELECTED, AdapterView.INVALID_POSITION)
            binding.etName.setText(it.getString(KEY_INPUT, ""))
        }
        contacts.sortWith(String.CASE_INSENSITIVE_ORDER)

        adapter = object : ArrayAdapter<String>(this, R.layout.item_contact, R.id.tvName, contacts) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = super.getView(position, convertView, parent)
                row.findViewById<TextView>(R.id.tvInitial).text =
                    getItem(position)?.firstOrNull()?.uppercase() ?: "?"
                return row
            }
        }
        binding.listContacts.adapter = adapter

        if (selectedPos in contacts.indices) {
            binding.listContacts.setItemChecked(selectedPos, true)
        } else {
            selectedPos = AdapterView.INVALID_POSITION
        }
        updateEmptyState()

        binding.listContacts.setOnItemClickListener { _, _, position, _ ->
            selectedPos = position
            binding.etName.setText(contacts[position])
            binding.etName.setSelection(binding.etName.text.length)
        }

        binding.listContacts.setOnItemLongClickListener { _, _, position, _ ->
            confirmDelete(position)
            true
        }

        binding.btnModify.setOnClickListener { modifySelected() }
        binding.btnAdd.setOnClickListener { addContact() }

        binding.etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                modifySelected()
                true
            } else {
                false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_CONTACTS, ArrayList(contacts))
        outState.putInt(KEY_SELECTED, selectedPos)
        outState.putString(KEY_INPUT, binding.etName.text.toString())
    }

    private fun modifySelected() {
        if (selectedPos == AdapterView.INVALID_POSITION) {
            toast(R.string.err_no_selection)
            return
        }
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.err_empty_name)
            return
        }
        if (isDuplicate(name, ignoreIndex = selectedPos)) {
            binding.etName.error = getString(R.string.err_duplicate)
            return
        }
        contacts[selectedPos] = name
        sortAndKeepSelection(name)
        toast(R.string.msg_modified)
    }

    private fun addContact() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.err_empty_name)
            return
        }
        if (isDuplicate(name, ignoreIndex = -1)) {
            binding.etName.error = getString(R.string.err_duplicate)
            return
        }
        contacts.add(name)
        sortAndKeepSelection(name)
        updateEmptyState()
        toast(R.string.msg_added)
    }

    private fun confirmDelete(position: Int) {
        val name = contacts[position]
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, name))
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_delete_yes) { _, _ ->
                contacts.removeAt(position)
                clearSelection()
                adapter.notifyDataSetChanged()
                updateEmptyState()
                toast(R.string.msg_deleted)
            }
            .show()
    }

    private fun isDuplicate(name: String, ignoreIndex: Int): Boolean =
        contacts.withIndex().any { (i, c) -> i != ignoreIndex && c.equals(name, ignoreCase = true) }

    /** Trie la liste, rafraîchit l'adaptateur et remet en surbrillance [name]. */
    private fun sortAndKeepSelection(name: String) {
        contacts.sortWith(String.CASE_INSENSITIVE_ORDER)
        adapter.notifyDataSetChanged()
        val idx = contacts.indexOf(name)
        if (idx >= 0) {
            selectedPos = idx
            binding.listContacts.setItemChecked(idx, true)
            binding.listContacts.smoothScrollToPosition(idx)
        }
    }

    private fun clearSelection() {
        selectedPos = AdapterView.INVALID_POSITION
        binding.listContacts.clearChoices()
        binding.etName.text.clear()
    }

    private fun updateEmptyState() {
        binding.tvEmpty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()

    private companion object {
        const val KEY_CONTACTS = "contacts"
        const val KEY_SELECTED = "selected"
        const val KEY_INPUT = "input"
    }
}
