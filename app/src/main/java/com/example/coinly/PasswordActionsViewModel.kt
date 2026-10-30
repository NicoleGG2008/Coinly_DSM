package com.example.coinly

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

enum class PasswordErrorField { EMAIL, CURRENT, NEW, GENERAL }
data class PasswordActionState(
    val busy: Boolean = false,
    val requestedEmail: String = "",
    val resetRequested: Boolean = false,
    val passwordChanged: Boolean = false,
    val errorRes: Int? = null,
    val errorField: PasswordErrorField = PasswordErrorField.GENERAL
)

/** Holds task state through recreation, without retaining an Activity or password. */
class PasswordActionsViewModel : ViewModel() {
    private val mutableState = MutableLiveData(PasswordActionState())
    val state: LiveData<PasswordActionState> = mutableState
    private val auth get() = FirebaseAuth.getInstance()
    fun editEmail() { if (mutableState.value?.busy != true) mutableState.value = PasswordActionState() }

    fun requestReset(email: String) {
        if (mutableState.value?.busy == true) return
        val target = email.trim()
        mutableState.value = PasswordActionState(busy = true, requestedEmail = target)
        auth.setLanguageCode("es")
        val task = try { auth.sendPasswordResetEmail(target) }
            catch (e: Exception) { Tasks.forException<Void>(e) }
        task.addOnCompleteListener { result ->
            val code = (result.exception as? FirebaseAuthException)?.errorCode
            // Keep the same neutral response whether or not an account exists.
            if (result.isSuccessful || code == "ERROR_USER_NOT_FOUND") {
                mutableState.value = PasswordActionState(requestedEmail = target, resetRequested = true)
            } else failure(result.exception, reset = true, email = target)
        }
    }

    fun changePassword(current: String, next: String) {
        if (mutableState.value?.busy == true) return
        if (!PasswordPolicy.valid(next)) {
            mutableState.value = PasswordActionState(errorRes = R.string.password_requirements, errorField = PasswordErrorField.NEW); return
        }
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank()) {
            mutableState.value = PasswordActionState(errorRes = R.string.password_session_missing); return
        }
        if (user.providerData.none { it.providerId == EmailAuthProvider.PROVIDER_ID }) {
            mutableState.value = PasswordActionState(errorRes = R.string.password_use_email_link); return
        }
        val uid = user.uid
        mutableState.value = PasswordActionState(busy = true)
        auth.setLanguageCode("es")
        val credential = EmailAuthProvider.getCredential(email, current)
        val task = try {
            user.reauthenticate(credential).continueWithTask { reauth ->
                if (!reauth.isSuccessful) throw reauth.exception ?: IllegalStateException()
                if (auth.currentUser?.uid != uid) throw IllegalStateException()
                user.updatePassword(next)
            }
        } catch (e: Exception) { Tasks.forException<Void>(e) }
        task.addOnCompleteListener { result ->
            if (result.isSuccessful && auth.currentUser?.uid == uid) {
                mutableState.value = PasswordActionState(passwordChanged = true)
            } else failure(result.exception, reset = false)
        }
    }

    private fun failure(error: Exception?, reset: Boolean, email: String = "") {
        val code = (error as? FirebaseAuthException)?.errorCode
        var field = PasswordErrorField.GENERAL
        val message = when {
            error is FirebaseNetworkException -> R.string.text_001
            error is FirebaseTooManyRequestsException -> R.string.text_002
            code == "ERROR_INVALID_EMAIL" -> { field = PasswordErrorField.EMAIL; R.string.text_005 }
            code == "ERROR_WEAK_PASSWORD" || code == "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS" -> {
                field = PasswordErrorField.NEW; R.string.password_policy_error
            }
            !reset && code in listOf("ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS") -> {
                field = PasswordErrorField.CURRENT; R.string.password_current_incorrect
            }
            code in listOf("ERROR_REQUIRES_RECENT_LOGIN", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN", "ERROR_USER_NOT_FOUND") -> R.string.password_session_missing
            code == "ERROR_OPERATION_NOT_ALLOWED" -> R.string.text_006
            code == "ERROR_USER_DISABLED" -> R.string.text_007
            code in listOf("ERROR_INVALID_API_KEY", "ERROR_APP_NOT_AUTHORIZED", "ERROR_INVALID_APP_CREDENTIAL", "ERROR_INTERNAL_ERROR", "ERROR_MISSING_RECAPTCHA_TOKEN", "ERROR_INVALID_RECAPTCHA_TOKEN") -> R.string.password_service_error
            reset -> R.string.password_reset_failed
            else -> R.string.password_change_failed
        }
        mutableState.value = PasswordActionState(requestedEmail = email, errorRes = message, errorField = field)
    }
}
