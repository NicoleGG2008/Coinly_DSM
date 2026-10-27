package com.example.coinly

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.coinly.data.MovementRepository.MovementRepository
import com.example.coinly.model.Movement
import com.example.coinly.ui.Tone
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/** Hojas inferiores para crear, editar y consultar movimientos. */
object TransactionDialog {

    /** Crea un movimiento (o edita [existing]) en una hoja inferior con validación por campo. */
    fun show(context: Context, initialType: String = Movement.TYPE_EXPENSE, existing: Movement? = null, onSaved: () -> Unit = {}) {
        val view = LayoutInflater.from(context).inflate(R.layout.sheet_transaction, null)
        val dialog = Ui.sheet(context, view)

        val title = view.findViewById<TextView>(R.id.tvSheetTitle)
        val btnIncome = view.findViewById<MaterialButton>(R.id.btnTypeIncome)
        val btnExpense = view.findViewById<MaterialButton>(R.id.btnTypeExpense)
        val saveButton = view.findViewById<MaterialButton>(R.id.btnSheetSave)
        val tilAmount = view.findViewById<TextInputLayout>(R.id.tilAmount)
        val tilCategory = view.findViewById<TextInputLayout>(R.id.tilCategory)
        val tilDescription = view.findViewById<TextInputLayout>(R.id.tilDescription)
        val tilDate = view.findViewById<TextInputLayout>(R.id.tilDate)
        val etAmount = view.findViewById<TextInputEditText>(R.id.etAmount)
        val acCategory = view.findViewById<MaterialAutoCompleteTextView>(R.id.acCategory)
        val etDescription = view.findViewById<TextInputEditText>(R.id.etDescription)
        val etDate = view.findViewById<TextInputEditText>(R.id.etDate)
        val acPayment = view.findViewById<MaterialAutoCompleteTextView>(R.id.acPayment)
        val etTags = view.findViewById<TextInputEditText>(R.id.etTags)

        fun isIncome() = btnIncome.isChecked

        // El botón de guardar toma el color del tipo: verde para ingresos, rojo para gastos
        fun tintSaveButton() {
            val c = ContextCompat.getColor(context, if (isIncome()) R.color.coinly_green_strong else R.color.coinly_danger)
            saveButton.backgroundTintList = ColorStateList.valueOf(c)
        }

        fun refreshCategories() {
            val list = if (isIncome()) MovementRepository.INCOME_CATEGORIES else MovementRepository.EXPENSE_CATEGORIES
            Ui.choices(acCategory, list.toTypedArray())
            if (acCategory.text.toString() !in list) acCategory.setText("", false)
        }

        Ui.choices(acPayment, MovementRepository.PAYMENT_METHODS.toTypedArray())
        Ui.bindDateField(context, etDate) { tilDate.error = null }
        tilDate.setEndIconOnClickListener { etDate.performClick() }
        Ui.clearErrorOnType(tilAmount, etAmount)
        Ui.clearErrorOnType(tilDescription, etDescription)
        acCategory.setOnItemClickListener { _, _, _, _ -> tilCategory.error = null }

        // Estado inicial
        val startType = existing?.type ?: initialType
        // Selección única controlada a mano: siempre queda exactamente un tipo activo
        fun selectType(income: Boolean) {
            btnIncome.isChecked = income
            btnExpense.isChecked = !income
            refreshCategories()
            tintSaveButton()
        }
        btnIncome.setOnClickListener { selectType(true) }
        btnExpense.setOnClickListener { selectType(false) }
        selectType(startType == Movement.TYPE_INCOME)
        etDate.setText(existing?.date ?: Ui.todayIso())

        if (existing != null) {
            title.text = S.text(R.string.text_068)
            etAmount.setText(existing.amount.toString())
            acCategory.setText(existing.category, false)
            etDescription.setText(existing.description)
            acPayment.setText(existing.paymentMethod, false)
            etTags.setText(existing.tags.joinToString(", "))
        }

        view.findViewById<View>(R.id.btnSheetCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.btnSheetSave).setOnClickListener {
            val amount = etAmount.text.toString().toDoubleOrNull()
            val category = acCategory.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val date = etDate.text.toString().trim()
            var valid = true

            if (amount == null || !amount.isFinite() || amount <= 0 || amount > 1000000000) { tilAmount.error = S.text(R.string.text_064); valid = false }
            if (category.isEmpty()) { tilCategory.error = S.text(R.string.text_063); valid = false }
            if (description.isEmpty() || description.length > 500) { tilDescription.error = S.text(R.string.text_020); valid = false }
            if (date.isEmpty()) { tilDate.error = S.text(R.string.text_069); valid = false }
            if (!valid) return@setOnClickListener

            val movement = Movement(
                id = existing?.id ?: "user-${System.currentTimeMillis()}",
                description = description,
                amount = amount!!,
                type = if (isIncome()) Movement.TYPE_INCOME else Movement.TYPE_EXPENSE,
                category = category,
                date = date,
                paymentMethod = acPayment.text.toString().trim(),
                tags = etTags.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            )
            if (movement.tags.size > 20 || movement.tags.any { it.length > 50 }) {
                etTags.error = S.text(R.string.tags_limit)
                return@setOnClickListener
            }
            val repo = MovementRepository(context)
            saveButton.isEnabled = false
            saveButton.text = S.text(R.string.saving)
            val task = if (existing == null) repo.addMovement(movement) else repo.updateMovement(movement)
            task.addOnCompleteListener {
                saveButton.isEnabled = true
                saveButton.text = S.text(R.string.text_065)
                if (it.isSuccessful) { dialog.dismiss(); onSaved() }
                else Ui.toast(view, Tone.DANGER, com.example.coinly.data.CloudStore.message(it.exception ?: IllegalStateException()))
            }
        }
        dialog.show()
    }

    /** Detalle de un movimiento con acciones de editar y eliminar. */
    fun showDetail(context: Context, movement: Movement, onChanged: () -> Unit) {
        val view = LayoutInflater.from(context).inflate(R.layout.sheet_movement_detail, null)
        val dialog = Ui.sheet(context, view)
        val income = movement.isIncome
        val tone = if (income) Tone.SUCCESS else Tone.DANGER
        val accent = ContextCompat.getColor(context, if (income) R.color.coinly_green_text else R.color.coinly_danger_text)

        view.findViewById<View>(R.id.layoutDetailIcon)
            .setBackgroundResource(if (income) R.drawable.bg_tile_income else R.drawable.bg_tile_expense)
        view.findViewById<ImageView>(R.id.ivDetailIcon).apply {
            setImageResource(if (income) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
            imageTintList = ColorStateList.valueOf(accent)
        }
        view.findViewById<TextView>(R.id.tvDetailAmount).apply {
            text = Ui.signedMoney(movement)
            setTextColor(ContextCompat.getColor(context, if (income) R.color.coinly_green_text else R.color.coinly_danger_text))
        }
        view.findViewById<TextView>(R.id.tvDetailDescription).text = movement.description
        view.findViewById<TextView>(R.id.tvDetailType).apply {
            text = if (income) S.text(R.string.text_070) else S.text(R.string.text_071)
            setBackgroundResource(tone.badge)
            val c = ContextCompat.getColor(context, tone.text)
            setTextColor(c)
            setCompoundDrawablesRelativeWithIntrinsicBounds(if (income) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down, 0, 0, 0)
            compoundDrawableTintList = ColorStateList.valueOf(c)
        }

        val rows = view.findViewById<LinearLayout>(R.id.layoutDetailRows)
        val details = listOfNotNull(
            S.text(R.string.text_072) to movement.category,
            S.text(R.string.text_073) to Ui.longDate(movement.date),
            movement.paymentMethod.takeIf { it.isNotBlank() }?.let { S.text(R.string.text_074) to it },
            movement.tags.takeIf { it.isNotEmpty() }?.let { S.text(R.string.text_075) to it.joinToString(", ") }
        )
        details.forEachIndexed { i, (label, value) ->
            if (i > 0) rows.addView(View(context).apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.coinly_divider))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(context, 1)))
            val row = LayoutInflater.from(context).inflate(R.layout.item_detail_row, rows, false)
            row.findViewById<TextView>(R.id.tvDetailLabel).text = label
            row.findViewById<TextView>(R.id.tvDetailValue).text = value
            rows.addView(row)
        }

        view.findViewById<View>(R.id.btnDetailEdit).setOnClickListener {
            dialog.dismiss()
            show(context, existing = movement, onSaved = onChanged)
        }
        view.findViewById<View>(R.id.btnDetailDelete).setOnClickListener {
            dialog.dismiss()
            Ui.confirm(context, S.text(R.string.text_076), S.text(R.string.text_077, movement.description), S.text(R.string.text_060)) {
                MovementRepository(context).deleteMovement(movement.id).addOnCompleteListener {
                    if (it.isSuccessful) onChanged()
                    else Ui.toast((context as android.app.Activity).findViewById(android.R.id.content), Tone.DANGER,
                        com.example.coinly.data.CloudStore.message(it.exception ?: IllegalStateException()))
                }
            }
        }
        dialog.show()
    }
}
