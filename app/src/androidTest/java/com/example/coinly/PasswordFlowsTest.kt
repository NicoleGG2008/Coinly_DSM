package com.example.coinly

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.gms.tasks.Tasks
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import org.hamcrest.Matchers.containsString
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PasswordFlowsTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val auth get() = FirebaseAuth.getInstance()
    private fun waitFor(label: String, condition: () -> Boolean) {
        val end=System.currentTimeMillis()+25000
        while(System.currentTimeMillis()<end) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if(condition()) return
            Thread.sleep(100)
        }
        throw AssertionError("Timeout: $label")
    }
    private fun field(id:Int,value:String) { onView(withId(id)).perform(scrollTo(),replaceText(value),closeSoftKeyboard()) }
    private fun <T:android.app.Activity> launch(type:Class<T>) = ActivityScenario.launch<T>(Intent(context,type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    private fun oobCodes():List<JSONObject> {
        val project=FirebaseApp.getInstance().options.projectId
        val connection=URL("http://10.0.2.2:9299/emulator/v1/projects/$project/oobCodes").openConnection() as HttpURLConnection
        connection.connectTimeout=5000; connection.readTimeout=5000
        try {
            val data=JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getJSONArray("oobCodes")
            return (0 until data.length()).map { data.getJSONObject(it) }
        } finally { connection.disconnect() }
    }
    @Before fun isolate() {
        assertTrue("Never run against production",BuildConfig.FIREBASE_EMULATOR)
        assertTrue("Requires separate QA app",context.packageName.endsWith(".passwordqa"))
        assertTrue(FirebaseApp.getInstance().options.projectId!!.startsWith("demo-"))
        auth.signOut()
    }
    @Test fun resetEmailLinkActuallyChangesPasswordAndStateSurvivesRecreation() {
        val email="reset-${System.currentTimeMillis()}@coinly.test"
        Tasks.await(auth.createUserWithEmailAndPassword(email,"Original123!"),15,TimeUnit.SECONDS)
        auth.signOut()
        launch(ForgotPasswordActivity::class.java).use { scenario ->
            field(R.id.etEmailForgot,"invalid")
            onView(withId(R.id.btnSendReset)).perform(scrollTo(),click())
            scenario.onActivity { assertEquals(it.getString(R.string.text_005),it.findViewById<TextInputLayout>(R.id.tilEmailForgot).error.toString()) }
            field(R.id.etEmailForgot,email)
            onView(withId(R.id.btnSendReset)).perform(scrollTo(),click())
            waitFor("reset response") {
                var sent=false; scenario.onActivity { sent=it.findViewById<View>(R.id.layoutForgotSent).visibility==View.VISIBLE }; sent
            }
            onView(withId(R.id.tvResetMessage)).check(matches(withText(containsString(email))))
            val code=oobCodes().first { it.optString("email")==email && it.optString("requestType")=="PASSWORD_RESET" }.getString("oobCode")
            assertEquals(email,Tasks.await(auth.verifyPasswordResetCode(code),15,TimeUnit.SECONDS))
            Tasks.await(auth.confirmPasswordReset(code,"Recovered456!"),15,TimeUnit.SECONDS)
            Tasks.await(auth.signInWithEmailAndPassword(email,"Recovered456!"),15,TimeUnit.SECONDS)
            assertEquals(email,auth.currentUser!!.email)
            scenario.recreate()
            onView(withId(R.id.layoutForgotSent)).check(matches(isDisplayed()))
            onView(withId(R.id.btnResetAgain)).perform(scrollTo(),click())
            onView(withId(R.id.layoutForgotForm)).check(matches(isDisplayed()))
            val missing="missing-${System.currentTimeMillis()}@coinly.test"
            field(R.id.etEmailForgot,missing)
            onView(withId(R.id.btnSendReset)).perform(scrollTo(),click())
            waitFor("neutral response for unregistered email") {
                var sent=false; scenario.onActivity { sent=it.findViewById<View>(R.id.layoutForgotSent).visibility==View.VISIBLE }; sent
            }
            onView(withId(R.id.tvResetMessage)).check(matches(withText(containsString("Si existe una cuenta"))))
            assertFalse(oobCodes().any { it.optString("email")==missing })
        }
    }
    @Test fun passwordChangeRejectsWrongPasswordAndConfirmsServerUpdate() {
        val email="change-${System.currentTimeMillis()}@coinly.test"
        val old="Original123!"; val next="Changed456!"
        val user=Tasks.await(auth.createUserWithEmailAndPassword(email,old),15,TimeUnit.SECONDS).user!!
        launch(ChangePasswordActivity::class.java).use { scenario ->
            field(R.id.etCurrentPassword,"Wrong123!"); field(R.id.etNewPassword,next); field(R.id.etConfirmNewPassword,next)
            onView(withId(R.id.btnSavePassword)).perform(scrollTo(),click())
            waitFor("wrong current password") {
                var rejected=false; scenario.onActivity {
                    rejected=it.findViewById<TextInputLayout>(R.id.tilCurrentPassword).error?.toString()==it.getString(R.string.password_current_incorrect)
                }; rejected
            }
            Tasks.await(auth.signInWithEmailAndPassword(email,old),15,TimeUnit.SECONDS)
            field(R.id.etCurrentPassword,old)
            onView(withId(R.id.btnSavePassword)).perform(scrollTo(),click())
            waitFor("change confirmation") {
                var changed=false; scenario.onActivity { changed=androidx.lifecycle.ViewModelProvider(it)[PasswordActionsViewModel::class.java].state.value?.passwordChanged==true }; changed
            }
            onView(withText(R.string.password_changed_title)).check(matches(isDisplayed()))
            auth.signOut()
            assertEquals(user.uid,Tasks.await(auth.signInWithEmailAndPassword(email,next),15,TimeUnit.SECONDS).user!!.uid)
            onView(withText(R.string.password_done)).perform(click())
        }
    }
}
