package com.example.coinly

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.example.coinly.data.*
import com.example.coinly.data.MovementRepository.MovementRepository
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.*

class EconomicEventActivity : BaseActivity() {
    private var eventId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uid = CloudStore.auth.currentUser?.uid ?: run { finish(); return }
        val existing = intent.getStringExtra("event_id")
        eventId = savedInstanceState?.getString("event_id") ?: existing ?: EconomicEvents.collection(uid).document().id
        setScreen(R.layout.activity_economic_editor)
        com.example.coinly.ui.EditorKeyboard.attach(findViewById(R.id.editorContent))
        findViewById<View>(R.id.editorBack).setOnClickListener { finish() }
        findViewById<View>(R.id.economicSessionHint).visibility = if (AuthSession.remember) View.GONE else View.VISIBLE
        val title = findViewById<TextInputEditText>(R.id.economicTitle)
        val amount = findViewById<TextInputEditText>(R.id.economicAmount)
        val type = findViewById<MaterialAutoCompleteTextView>(R.id.economicType)
        val category = findViewById<MaterialAutoCompleteTextView>(R.id.economicCategory)
        val date = findViewById<TextInputEditText>(R.id.economicDate)
        val message = findViewById<TextView>(R.id.editorError)
        val save = findViewById<MaterialButton>(R.id.editorSave)
        val delete = findViewById<View>(R.id.economicDelete)
        val view = findViewById<View>(R.id.economicViewMovement)
        type.setSimpleItems(arrayOf(getString(R.string.economic_income), getString(R.string.economic_expense)))
        fun updateCategories(reset: Boolean) {
            val values = if (type.text.toString() == getString(R.string.economic_income)) MovementRepository.INCOME_CATEGORIES else MovementRepository.EXPENSE_CATEGORIES
            category.setSimpleItems(values.toTypedArray())
            if (reset || category.text.toString() !in values) category.setText(values.first(), false)
        }
        type.doAfterTextChanged { updateCategories(false) }
        if (savedInstanceState == null) {
            type.setText(getString(R.string.economic_expense), false)
            val selected = intent.getStringExtra("event_date")?.takeIf { NewsDates.valid(it) && it >= EconomicEvents.today() }
            date.setText(selected ?: EconomicEvents.today())
        }
        updateCategories(false)
        date.setOnClickListener {
            val parts = date.text.toString().split("-").mapNotNull { it.toIntOrNull() }
            val today = EconomicEvents.today().split("-").map { it.toInt() }
            android.app.DatePickerDialog(this, { _, y, m, d ->
                date.setText(String.format(java.util.Locale.ROOT, "%04d-%02d-%02d", y, m + 1, d))
            }, parts.getOrElse(0) { today[0] }, parts.getOrElse(1) { today[1] } - 1, parts.getOrElse(2) { today[2] }).apply {
                datePicker.minDate = EconomicEvents.dueAt(EconomicEvents.today())!!.toDate().time
            }.show()
        }
        val model = ViewModelProvider(this)[EconomicEventEditor::class.java]
        var bound = savedInstanceState != null
        model.state.observe(this) { state ->
            val data = state.data
            if (!bound && data != null) {
                title.setText(data.title)
                amount.setText(java.math.BigDecimal.valueOf(data.amount).stripTrailingZeros().toPlainString())
                type.setText(if (data.type == "income") getString(R.string.economic_income) else getString(R.string.economic_expense), false)
                category.setText(data.category, false); date.setText(data.date); bound = true
            }
            listOf<View>(title, amount, type, category, date, save).forEach { it.isEnabled = state.ready && !state.busy && data?.posted != true }
            delete.visibility = if (existing == null) View.GONE else View.VISIBLE
            delete.isEnabled = state.ready && !state.busy
            save.visibility = if (data?.posted == true) View.GONE else View.VISIBLE
            save.setText(if (state.busy) R.string.economic_saving else R.string.economic_save)
            view.visibility = if (data?.posted == true) View.VISIBLE else View.GONE
            message.text = state.error?.let { getString(it) } ?: if (data?.posted == true) getString(R.string.economic_posted) else ""
            message.visibility = if (message.text.isEmpty()) View.GONE else View.VISIBLE
            if (state.done) {
                if (!BuildConfig.FIREBASE_EMULATOR) EconomicEventWork.configure(this, uid)
                setResult(RESULT_OK, Intent().putExtra("saved_date", state.data?.date ?: date.text.toString()))
                finish()
            }
        }
        model.load(eventId, existing != null)
        save.setOnClickListener {
            val value = amount.text.toString().trim()
            val parsed = if (value.matches(Regex("[0-9]+([.,][0-9]{1,2})?"))) value.replace(',', '.').toDoubleOrNull() else null
            val titleInvalid = title.text.toString().trim().isBlank()
            findViewById<TextInputLayout>(R.id.tilEconomicTitle).error = if (titleInvalid) getString(R.string.text_020) else null
            val amountInvalid = parsed == null || !parsed.isFinite() || parsed <= 0 || parsed > 1_000_000_000
            findViewById<TextInputLayout>(R.id.tilEconomicAmount).error = if (amountInvalid) getString(R.string.economic_amount_help) else null
            if (titleInvalid || amountInvalid) { (if (titleInvalid) title else amount).requestFocus(); return@setOnClickListener }
            val eventType = if (type.text.toString() == getString(R.string.economic_income)) "income" else "expense"
            model.save(EconomicEvent(eventId, title.text.toString().trim(), parsed!!, eventType, category.text.toString(), date.text.toString()))
        }
        delete.setOnClickListener { MaterialAlertDialogBuilder(this).setMessage(R.string.economic_delete_confirm)
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok) { _, _ -> model.delete(eventId) }.show() }
        view.setOnClickListener {
            val movement = MovementRepository(this).getById(model.state.value?.data?.movementId.orEmpty())
            if (movement != null) TransactionDialog.showDetail(this, movement) {}
            else message.apply { setText(R.string.economic_error); visibility = View.VISIBLE }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("event_id", eventId); super.onSaveInstanceState(outState) }
}
