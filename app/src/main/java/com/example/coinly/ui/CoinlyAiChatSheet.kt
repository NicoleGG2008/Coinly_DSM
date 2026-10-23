package com.example.coinly.ui

import android.content.Context
import android.content.res.Configuration
import android.text.Html
import android.text.Spanned
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.example.coinly.R
import com.example.coinly.data.UserPrefs
import com.example.coinly.data.ai.CoinlyAiAssistant
import com.example.coinly.model.ChatMessage
import com.example.coinly.model.ChatSource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip

object CoinlyAiChatSheet {

    private val sessionHistory = mutableListOf<ChatMessage>()
    var isShowing: Boolean = false
        private set

    private var activeDialog: BottomSheetDialog? = null

    fun show(context: Context) {
        if (isShowing && activeDialog?.isShowing == true) {
            activeDialog?.dismiss()
        }

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.sheet_coinly_ai_chat, null)

        val dialog = BottomSheetDialog(context, R.style.ThemeOverlay_Coinly_BottomSheetDialog).apply {
            setContentView(view)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        activeDialog = dialog
        isShowing = true

        dialog.setOnDismissListener {
            isShowing = false
            activeDialog = null
        }

        // Expandir la hoja según orientación (portrait/landscape/tablet)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val heightFactor = if (isLandscape) 0.95 else 0.88
                it.layoutParams.height = (context.resources.displayMetrics.heightPixels * heightFactor).toInt()
                it.requestLayout()
            }
        }

        val assistant = CoinlyAiAssistant(context)
        val prefs = UserPrefs(context)

        val messagesContainer = view.findViewById<LinearLayout>(R.id.layoutChatMessages)
        val scrollChat = view.findViewById<NestedScrollView>(R.id.scrollChat)
        val typingIndicator = view.findViewById<LinearLayout>(R.id.layoutTypingIndicator)
        val suggestionsGroup = view.findViewById<LinearLayout>(R.id.layoutSuggestionsGroup)
        val etMessage = view.findViewById<EditText>(R.id.etChatMessage)
        val btnSend = view.findViewById<View>(R.id.btnSendChat)
        val btnClose = view.findViewById<View>(R.id.btnCloseChat)
        val tvStatus = view.findViewById<TextView>(R.id.tvAiStatusBadge)

        btnClose.setOnClickListener { dialog.dismiss() }

        // Mensaje inicial de bienvenida si la sesión está limpia
        if (sessionHistory.isEmpty()) {
            val userName = prefs.name.ifBlank { "amigo" }
            val welcomeText = "¡Hola, **$userName**! 👋 Soy **Coinly AI**, tu asistente financiero personal.\n\n" +
                "¿En qué te puedo ayudar hoy? Toca una sugerencia rápida o escribe tu consulta."
            sessionHistory.add(
                ChatMessage(
                    text = welcomeText,
                    isUser = false,
                    source = ChatSource.GEMINI
                )
            )
        }

        // Renderizar historial actual
        fun renderMessages() {
            messagesContainer.removeAllViews()
            sessionHistory.forEach { msg ->
                messagesContainer.addView(createBubbleView(context, msg))
            }
            scrollChat.post {
                scrollChat.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }

        renderMessages()

        // Enviar mensaje
        fun sendMessage(text: String) {
            val cleanText = text.trim()
            if (cleanText.isEmpty()) return

            etMessage.setText("")

            val userMsg = ChatMessage(text = cleanText, isUser = true)
            sessionHistory.add(userMsg)
            renderMessages()

            // Mostrar estado de escribiendo...
            typingIndicator.visibility = View.VISIBLE
            typingIndicator.announceForAccessibility("Coinly AI está procesando tu consulta...")
            scrollChat.post { scrollChat.fullScroll(ScrollView.FOCUS_DOWN) }

            btnSend.isEnabled = false

            assistant.ask(cleanText, sessionHistory) { responseMsg ->
                sessionHistory.add(responseMsg)
                typingIndicator.visibility = View.GONE
                btnSend.isEnabled = true

                // Actualizar badge de origen (Gemini real vs Modo Local por reglas)
                tvStatus.text = if (responseMsg.source == ChatSource.GEMINI)
                    "Asistente con Gemini AI"
                else
                    "Asistente local (Modo respaldo)"

                renderMessages()

                messagesContainer.announceForAccessibility("Respuesta de Coinly AI: " + responseMsg.text)
            }
        }

        btnSend.setOnClickListener { sendMessage(etMessage.text.toString()) }

        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage(etMessage.text.toString())
                true
            } else false
        }

        // Configurar Sugerencias Rápidas
        val suggestions = listOf(
            "¿Cómo va mi presupuesto?",
            "¿En qué he gastado más?",
            "Consejos para ahorrar",
            "¿Puedo gastar $50 en ocio?"
        )

        suggestionsGroup.removeAllViews()
        suggestions.forEach { prompt ->
            val chip = Chip(context).apply {
                text = prompt
                isClickable = true
                isFocusable = true
                setChipBackgroundColorResource(R.color.coinly_tint_info)
                setTextColor(ContextCompat.getColor(context, R.color.coinly_accent))
                setChipStrokeColorResource(R.color.coinly_accent)
                chipStrokeWidth = dp(context, 1).toFloat()
                contentDescription = "Sugerencia: $prompt"
                setOnClickListener { sendMessage(prompt) }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(context, 8)
            }
            suggestionsGroup.addView(chip, lp)
        }

        dialog.show()

        // Foco inicial
        etMessage.requestFocus()
    }

    private fun createBubbleView(context: Context, msg: ChatMessage): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (msg.isUser) Gravity.END else Gravity.START
            setPadding(0, dp(context, 4), 0, dp(context, 6))
        }

        val maxWidth = (context.resources.displayMetrics.widthPixels * 0.82).toInt()

        val bubbleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10))

            if (msg.isUser) {
                setBackgroundResource(R.drawable.bg_bubble_user)
            } else {
                setBackgroundResource(R.drawable.bg_bubble_ai)
            }
        }

        // Encabezado pequeño dentro de la burbuja para la AI
        if (!msg.isUser) {
            val headerLabel = TextView(context).apply {
                val tag = if (msg.source == ChatSource.GEMINI) "Coinly AI ✨" else "Coinly AI · Modo local 🛡️"
                text = tag
                setTextAppearance(R.style.TextAppearance_Coinly_Caption)
                setTextColor(ContextCompat.getColor(context, R.color.coinly_accent))
                textStyleBold()
                setPadding(0, 0, 0, dp(context, 4))
            }
            bubbleContainer.addView(headerLabel)
        }

        val textMsg = TextView(context).apply {
            text = formatMarkdownText(msg.text)
            setTextAppearance(R.style.TextAppearance_Coinly_Body)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (msg.isUser) R.color.coinly_on_dark else R.color.coinly_text_primary
                )
            )
            setMaxWidth(maxWidth)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        bubbleContainer.addView(textMsg)

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            if (msg.isUser) {
                gravity = Gravity.END
            } else {
                gravity = Gravity.START
            }
        }

        layout.addView(bubbleContainer, lp)
        return layout
    }

    private fun formatMarkdownText(raw: String): Spanned {
        val html = raw
            .replace("\n", "<br/>")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
            .replace(Regex("(?m)^[•\\-] (.*)$"), "• $1")
        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    }

    private fun TextView.textStyleBold() {
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
