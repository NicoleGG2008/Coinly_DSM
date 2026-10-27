package com.example.coinly
import android.app.Activity
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.activity.ComponentActivity
import com.example.coinly.ui.Ui
import com.example.coinly.ui.Tone
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException

/** Retains the operation through configuration changes; never retains an Activity. */
class AuthOperation : ViewModel() {
    var task: Task<*>? = null
}
object AuthUi {
    fun message(e: Exception?): String = when (e) {
        is FirebaseNetworkException -> S.text(R.string.text_001)
        is FirebaseTooManyRequestsException -> S.text(R.string.text_002)
        is FirebaseAuthException -> when(e.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> S.text(R.string.text_003)
            "ERROR_WEAK_PASSWORD" -> S.text(R.string.text_004)
            "ERROR_INVALID_EMAIL" -> S.text(R.string.text_005)
            "ERROR_OPERATION_NOT_ALLOWED" -> S.text(R.string.text_006)
            "ERROR_REQUIRES_RECENT_LOGIN" -> S.text(R.string.auth_recent)
            "ERROR_USER_DISABLED" -> S.text(R.string.text_007)
            else -> S.text(R.string.text_008)
        }
        else -> S.text(R.string.text_009)
    }
    fun <T> run(activity: ComponentActivity, button: TextView, action: () -> Task<T>, success: () -> Unit) {
        val model = ViewModelProvider(activity)[AuthOperation::class.java]
        if (model.task?.isComplete == false) return
        val label = button.text
        button.isEnabled = false; button.text = S.text(R.string.processing)
        val task = try { action() } catch (e: Exception) { com.google.android.gms.tasks.Tasks.forException<T>(e) }
        model.task = task
        task.addOnCompleteListener {
            if (activity.isDestroyed || activity.isFinishing) return@addOnCompleteListener
            model.task = null
            button.isEnabled = true; button.text = label
            if (it.isSuccessful) success() else {
                if (activity is LoginActivity || activity is RegisterActivity) com.example.coinly.data.AuthSession.logout()
                Ui.toast(activity.findViewById(android.R.id.content), Tone.DANGER, message(it.exception))
            }
        }
    }
}
