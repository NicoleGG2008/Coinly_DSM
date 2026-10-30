package com.example.coinly.ui
import com.example.coinly.S

import android.content.Context
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.coinly.R
import com.example.coinly.data.UserPrefs
import com.example.coinly.data.ai.CoinlyAiAssistant
import com.example.coinly.model.ChatMessage
import com.example.coinly.model.ChatSource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip

object CoinlyAiChatSheet {

    private var historyOwner: String? = null
    private val sessionHistory = mutableListOf<ChatMessage>()
    fun clearSession() { historyOwner = null; sessionHistory.clear() }

    fun show(context: Context) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (historyOwner != uid) { sessionHistory.clear(); historyOwner = uid }
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.sheet_coinly_ai_chat, null)

        val dialog = BottomSheetDialog(context, R.style.ThemeOverlay_Coinly_BottomSheetDialog).apply {
            setContentView(view)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        // Expandir la hoja para aprovechar el espacio en pantalla
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                it.layoutParams.height = (context.resources.displayMetrics.heightPixels * 0.88).toInt()
                it.requestLayout()
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    dialog.window?.let { window ->
                        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                    }
                    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { content, insets ->
                        val keyboard = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom
                        val navigation = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
                        content.setPadding(content.paddingLeft, content.paddingTop, content.paddingRight, maxOf(keyboard, navigation))
                        content.findViewById<View>(R.id.layoutSuggestionsGroup).visibility = if (keyboard > 0) View.GONE else View.VISIBLE
                        insets
                    }
                    androidx.core.view.ViewCompat.requestApplyInsets(view)
                }
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
        val accessHelp = com.google.android.material.button.MaterialButton(context).apply {
            setText(R.string.ai_device_help); visibility = View.GONE
            setOnClickListener { com.example.coinly.AiAppCheck.showHelp(context) }
        }
        (view as LinearLayout).let { root -> root.addView(accessHelp, root.childCount - 1) }

        var inFlight = false
        var pendingId: String? = null
        val owner = context as? LifecycleOwner
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) dialog.dismiss()
        }
        owner?.lifecycle?.addObserver(lifecycleObserver)
        dialog.setOnDismissListener {
            owner?.lifecycle?.removeObserver(lifecycleObserver)
            assistant.close()
            pendingId?.let { id -> sessionHistory.removeAll { it.id == id } }
        }
        tvStatus.text = when (sessionHistory.lastOrNull()?.source) {
            ChatSource.GEMINI -> S.text(R.string.text_115)
            ChatSource.RULE_BASED -> S.text(R.string.ai_local)
            else -> S.text(R.string.ai_ready)
        }
        btnClose.setOnClickListener { dialog.dismiss() }

        // Mensaje inicial de bienvenida si la sesión está limpia
        if (sessionHistory.isEmpty()) {
            val userName = prefs.name.ifBlank { "amigo" }
            val welcomeText = S.text(R.string.text_112, userName) +
                S.text(R.string.text_113)
            sessionHistory.add(
                ChatMessage(
                    text = welcomeText,
                    isUser = false,
                    source = ChatSource.ASSISTANT
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
            if (cleanText.isEmpty() || inFlight) return
            inFlight = true
            tvStatus.text = S.text(R.string.ai_connecting)
            accessHelp.visibility = View.GONE

            etMessage.setText("")

            val userMsg = ChatMessage(text = cleanText, isUser = true)
            pendingId = userMsg.id
            sessionHistory.add(userMsg)
            renderMessages()

            // Mostrar estado de escribiendo...
            typingIndicator.visibility = View.VISIBLE
            typingIndicator.announceForAccessibility(S.text(R.string.text_114))
            scrollChat.post { scrollChat.fullScroll(ScrollView.FOCUS_DOWN) }

            btnSend.isEnabled = false

            assistant.ask(cleanText, sessionHistory.toList()) { responseMsg ->
                if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != uid ||
                    historyOwner != uid || !dialog.isShowing) return@ask
                pendingId = null
                inFlight = false
                sessionHistory.add(responseMsg)
                accessHelp.visibility = if (responseMsg.noticeRes in listOf(R.string.ai_device_access, R.string.ai_error_access)) View.VISIBLE else View.GONE
                typingIndicator.visibility = View.GONE
                btnSend.isEnabled = true

                // Actualizar badge de origen (Gemini real vs Modo Local por reglas)
                tvStatus.text = when (responseMsg.source) {
                    ChatSource.GEMINI -> S.text(R.string.text_115)
                    ChatSource.RULE_BASED -> S.text(R.string.ai_local)
                    else -> S.text(R.string.ai_ready)
                }

                renderMessages()

                messagesContainer.announceForAccessibility(S.text(R.string.text_117) + responseMsg.text)
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
            S.text(R.string.text_118),
            S.text(R.string.text_119),
            S.text(R.string.text_120),
            S.text(R.string.text_121)
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
                contentDescription = S.text(R.string.text_122, prompt)
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
                val tag = when (msg.source) {
                    ChatSource.GEMINI -> S.text(R.string.text_123)
                    ChatSource.RULE_BASED -> S.text(R.string.ai_local)
                    else -> S.text(R.string.ai_name)
                }
                text = tag
                setTextAppearance(R.style.TextAppearance_Coinly_Caption)
                setTextColor(ContextCompat.getColor(context, R.color.coinly_accent))
                textStyleBold()
                setPadding(0, 0, 0, dp(context, 4))
            }
            bubbleContainer.addView(headerLabel)
        }

        msg.noticeRes?.let { reason ->
            bubbleContainer.addView(TextView(context).apply {
                setText(reason)
                setTextAppearance(R.style.TextAppearance_Coinly_Caption)
                setTextColor(ContextCompat.getColor(context, R.color.coinly_text_secondary))
                setPadding(0, 0, 0, dp(context, 8))
            })
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
        val html = android.text.TextUtils.htmlEncode(raw)
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
