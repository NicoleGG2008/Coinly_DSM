package com.example.coinly

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.example.coinly.data.NewsPublishing
import com.example.coinly.data.NewsRepository
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.*

class NewsEditorActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreen(R.layout.activity_news_editor)
        com.example.coinly.ui.EditorKeyboard.attach(findViewById(R.id.editorContent))
        findViewById<View>(R.id.editorBack).setOnClickListener { finish() }
        val title = findViewById<TextInputEditText>(R.id.newsEditorTitle)
        val summary = findViewById<TextInputEditText>(R.id.newsEditorSummary)
        val body = findViewById<TextInputEditText>(R.id.newsEditorBody)
        val category = findViewById<MaterialAutoCompleteTextView>(R.id.newsEditorCategory)
        val event = findViewById<TextInputEditText>(R.id.newsEditorEvent)
        val clearDate = findViewById<View>(R.id.newsClearEvent)
        val error = findViewById<TextView>(R.id.editorError)
        val save = findViewById<MaterialButton>(R.id.editorSave)
        category.setSimpleItems(NewsRepository.categories.drop(1).toTypedArray())
        if (savedInstanceState == null) category.setText(NewsRepository.categories[1], false)
        event.setOnClickListener {
            val parts = event.text.toString().split("-").mapNotNull { it.toIntOrNull() }
            val now = java.util.Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d -> event.setText(String.format(java.util.Locale.ROOT, "%04d-%02d-%02d", y, m + 1, d)) },
                parts.getOrElse(0) { now.get(java.util.Calendar.YEAR) }, parts.getOrElse(1) { now.get(java.util.Calendar.MONTH) + 1 } - 1,
                parts.getOrElse(2) { now.get(java.util.Calendar.DAY_OF_MONTH) }).show()
        }
        fun renderClear() { clearDate.visibility = if (event.text.isNullOrBlank()) View.GONE else View.VISIBLE }
        event.doAfterTextChanged { renderClear() }; clearDate.setOnClickListener { event.text?.clear() }; renderClear()
        val model = ViewModelProvider(this)[NewsPublishing::class.java]
        save.setOnClickListener {
            val fields = listOf(Triple(title, R.id.tilNewsEditorTitle, 140), Triple(summary, R.id.tilNewsEditorSummary, 500), Triple(body, R.id.tilNewsEditorBody, 10000))
            var first: View? = null
            fields.forEach { (field, layout, max) ->
                val invalid = field.text.toString().trim().length !in 1..max
                findViewById<TextInputLayout>(layout).error = if (invalid) getString(R.string.news_field_required, max) else null
                if (invalid && first == null) first = field
            }
            if (first != null) { first?.requestFocus(); return@setOnClickListener }
            model.publish(title.text.toString().trim(), summary.text.toString().trim(), body.text.toString().trim(), category.text.toString(), event.text.toString())
        }
        model.state.observe(this) { state ->
            listOf<View>(title, summary, body, category, event, clearDate, save).forEach { it.isEnabled = !state.busy && !state.done }
            save.setText(if (state.busy) R.string.news_publishing else R.string.news_publish)
            error.text = state.error?.let { getString(it) }.orEmpty()
            error.visibility = if (state.error == null) View.GONE else View.VISIBLE
            if (state.done) {
                setResult(RESULT_OK, Intent()); finish()
            }
        }
    }
}
