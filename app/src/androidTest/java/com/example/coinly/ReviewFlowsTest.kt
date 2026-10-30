package com.example.coinly

import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.assertion.ViewAssertions.matches
import com.example.coinly.data.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/** Refuses to touch the production app, accounts or database. */
@RunWith(AndroidJUnit4::class)
class ReviewFlowsTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private fun <T> await(task: com.google.android.gms.tasks.Task<T>) = Tasks.await(task, 25, TimeUnit.SECONDS)
    private fun screenshot(name: String) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }
    private fun waitFor(label: String, check: () -> Boolean) {
        val end = System.currentTimeMillis() + 25000
        while (System.currentTimeMillis() < end) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if (check()) return
            Thread.sleep(100)
        }
        throw AssertionError("Timeout: $label")
    }
    @Before fun localOnly() {
        assumeTrue(BuildConfig.FIREBASE_EMULATOR && context.packageName.endsWith(".reviewqa"))
        assumeTrue(FirebaseApp.getInstance().options.projectId == "demo-coinly-review")
        auth.signOut()
        await(auth.createUserWithEmailAndPassword("review-${System.nanoTime()}@coinly.test", "Original123!"))
    }

    private fun resumedActivity(): android.app.Activity? {
        var activity: android.app.Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).firstOrNull()
        }
        return activity
    }

    private fun assertNavigation(vararg labels: Int) {
        val activity = resumedActivity()!!
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val bar = activity.findViewById<android.widget.LinearLayout>(R.id.nav_bar)
            assertEquals(labels.size, bar.childCount)
            assertEquals(labels.map(context::getString), (0 until bar.childCount).map { bar.getChildAt(it).contentDescription.toString() })
        }
    }

    @Test fun dedicatedNewsAccountLandsInNewsAndOnlyHasNewsAndProfile() {
        auth.signOut()
        await(auth.createUserWithEmailAndPassword("coinlyadministrador@gmail.com", "Original123!"))
        // Only the synthetic account in the local Auth emulator is used here.
        ActivityScenario.launch<BienvenidaActivity>(Intent(context, BienvenidaActivity::class.java)).use {
            waitFor("administrator lands in news") { resumedActivity() is NoticiasActivity }
            assertNavigation(R.string.text_016, R.string.text_017)
            onView(withContentDescription(R.string.text_019)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
            onView(withContentDescription(context.getString(R.string.text_017))).perform(click())
            waitFor("administrator profile") { resumedActivity() is PerfilActivity }
            assertNavigation(R.string.text_016, R.string.text_017)
        }
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use {
            waitFor("old home redirects to news") { resumedActivity() is NoticiasActivity }
            assertNavigation(R.string.text_016, R.string.text_017)
            onView(withId(R.id.btnTabCalendar)).perform(click())
            onView(withId(R.id.btnAddEconomicEvent)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test fun regularAccountKeepsAllTabsWithoutPermissionRefresh() {
        ActivityScenario.launch<NoticiasActivity>(Intent(context, NoticiasActivity::class.java)).use { scenario ->
            waitFor("reader role loaded") {
                var ready = false
                scenario.onActivity { ready = it.findViewById<TextView>(R.id.newsRoleStatus).text.toString() == context.getString(R.string.news_role_reader) }
                ready
            }
            assertNavigation(R.string.text_013, R.string.text_014, R.string.text_015, R.string.text_016, R.string.text_017)
            onView(withId(R.id.btnRefreshNewsRole)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.btnTabCalendar)).perform(click())
            onView(withId(R.id.btnAddEconomicEvent)).check(matches(isDisplayed()))
        }
    }

    @Test fun concurrentExecutionCreatesOneMovementAndRejectsFutureExecution() = runBlocking {
        val uid = auth.currentUser!!.uid
        val today = EconomicEvents.today()
        val doc = EconomicEvents.collection(uid).document("due")
        await(doc.set(mapOf("title" to "Gasto QA", "amount" to 20.0, "type" to "expense", "category" to "Comida",
            "date" to today, "dueAt" to EconomicEvents.dueAt(today), "status" to "pending", "movementId" to "", "createdAt" to FieldValue.serverTimestamp())))
        await(Tasks.whenAll(EconomicEvents.post(uid, "due"), EconomicEvents.post(uid, "due")))
        EconomicEvents.processDue(uid)
        val movements = await(db.collection("usuarios").document(uid).collection("movimientos").get(Source.SERVER))
        assertEquals(1, movements.size())
        assertEquals("prog_due", movements.documents.single().id)
        assertEquals(20.0, movements.documents.single().getDouble("amount")!!, 0.0)
        assertEquals("posted", await(doc.get(Source.SERVER)).getString("status"))
        val futureDate = "2099-12-01"
        val future = EconomicEvents.collection(uid).document("future")
        await(future.set(mapOf("title" to "Ingreso futuro", "amount" to 50.0, "type" to "income", "category" to "Trabajo",
            "date" to futureDate, "dueAt" to EconomicEvents.dueAt(futureDate), "status" to "pending", "movementId" to "", "createdAt" to FieldValue.serverTimestamp())))
        try { await(EconomicEvents.post(uid, "future")); fail("Future movement must not be created") } catch (_: java.util.concurrent.ExecutionException) {}
        assertFalse(await(db.collection("usuarios").document(uid).collection("movimientos").document("prog_future").get(Source.SERVER)).exists())
    }

    @Test fun calendarRetainsMonthTabAndSelectedDayAcrossRecreation() {
        ActivityScenario.launch<NoticiasActivity>(Intent(context, NoticiasActivity::class.java)).use { scenario ->
            onView(withId(R.id.btnTabCalendar)).perform(click())
            onView(withId(R.id.btnNextMonth)).perform(scrollTo(), click())
            var month = ""
            scenario.onActivity { month = it.findViewById<TextView>(R.id.tvMonthTitle).text.toString() }
            onView(withText("15")).perform(scrollTo(), click())
            scenario.recreate()
            scenario.onActivity {
                assertEquals(month, it.findViewById<TextView>(R.id.tvMonthTitle).text.toString())
                assertEquals(android.view.View.VISIBLE, it.findViewById<android.view.View>(R.id.layoutCalendarSection).visibility)
                assertTrue(it.findViewById<TextView>(R.id.tvEventsTitle).text.toString().contains("15"))
            }
        }
    }

    @Test fun resetEmailCodeActuallyChangesThePassword() {
        val email = auth.currentUser!!.email!!
        auth.signOut()
        ActivityScenario.launch<ForgotPasswordActivity>(Intent(context, ForgotPasswordActivity::class.java)).use { scenario ->
            onView(withId(R.id.etEmailForgot)).perform(replaceText(email), closeSoftKeyboard())
            onView(withId(R.id.btnSendReset)).perform(scrollTo(), click())
            waitFor("reset link requested") {
                var ready = false
                scenario.onActivity { ready = it.findViewById<android.view.View>(R.id.layoutForgotSent).visibility == android.view.View.VISIBLE }
                ready
            }
            val connection = URL("http://10.0.2.2:9199/emulator/v1/projects/demo-coinly-review/oobCodes").openConnection() as HttpURLConnection
            val code: String
            try {
                connection.connectTimeout = 5000; connection.readTimeout = 5000
                val data = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getJSONArray("oobCodes")
                code = (0 until data.length()).map { data.getJSONObject(it) }.last { it.optString("email") == email && it.optString("requestType") == "PASSWORD_RESET" }.getString("oobCode")
            } finally { connection.disconnect() }
            assertEquals(email, await(auth.verifyPasswordResetCode(code)))
            await(auth.confirmPasswordReset(code, "Recovered456!"))
            assertEquals(email, await(auth.signInWithEmailAndPassword(email, "Recovered456!")).user!!.email)
        }
    }

    @Test fun editorPublishesOnlyWithTrustedAdministratorClaim() {
        val uid = auth.currentUser!!.uid
        val connection = URL("http://10.0.2.2:9199/identitytoolkit.googleapis.com/v1/projects/demo-coinly-review/accounts:update").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"; connection.doOutput = true; connection.connectTimeout = 5000; connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer owner"); connection.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().put("localId", uid).put("customAttributes", "{\"newsAdmin\":true}").toString()
            connection.outputStream.use { it.write(body.toByteArray()) }
            assertEquals(200, connection.responseCode)
        } finally { connection.disconnect() }
        assertEquals(true, await(auth.currentUser!!.getIdToken(true)).claims["newsAdmin"])
        val title = "Noticia QA ${System.nanoTime()}"
        ActivityScenario.launch<NoticiasActivity>(Intent(context, NoticiasActivity::class.java)).use { scenario ->
            waitFor("administrator action enabled") {
                var enabled = false
                scenario.onActivity { enabled = it.findViewById<android.view.View>(R.id.btnAddNews).isEnabled }
                enabled
            }
            screenshot("administrator-news-actions")
            onView(withId(R.id.btnAddNews)).perform(click())
            fun field(id: Int, value: String) { onView(withId(id)).perform(scrollTo(), replaceText(value), closeSoftKeyboard()) }
            field(R.id.newsEditorTitle, title); field(R.id.newsEditorSummary, "Resumen de prueba"); field(R.id.newsEditorBody, "Contenido de prueba")
            onView(withId(R.id.newsEditorCategory)).perform(scrollTo(), click())
            onView(withText(NewsRepository.categories[2])).inRoot(isPlatformPopup()).perform(click())
            onView(withId(R.id.newsEditorEvent)).perform(scrollTo(), click())
            onView(withId(android.R.id.button1)).perform(click())
            onView(withId(R.id.newsEditorEvent)).check(matches(withText(EconomicEvents.today())))
            screenshot("news-editor")
            onView(withId(R.id.editorSave)).perform(click())
            waitFor("published news") {
                await(db.collection("noticias").whereEqualTo("published", true).get(Source.SERVER)).documents.any { it.getString("title") == title }
            }
            waitFor("returned to news list") {
                var visible = false
                scenario.onActivity { visible = it.findViewById<android.view.View>(R.id.btnAddNews).isEnabled }
                visible
            }
            onView(org.hamcrest.Matchers.allOf(withId(R.id.tvNewsTitle), withText(title))).perform(scrollTo()).check(matches(isDisplayed()))
            screenshot("published-news")
            onView(withId(R.id.btnTabCalendar)).perform(click())
            onView(org.hamcrest.Matchers.allOf(withId(R.id.tvEventTitle), withText(title))).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test fun economicEditorCreatesEditsAndDeletesTheProgramming() {
        val uid = auth.currentUser!!.uid
        val title = "Programación QA ${System.nanoTime()}"
        ActivityScenario.launch<EconomicEventActivity>(Intent(context, EconomicEventActivity::class.java)).use { scenario ->
            onView(withId(R.id.economicTitle)).perform(scrollTo(), replaceText(title), closeSoftKeyboard())
            onView(withId(R.id.economicAmount)).perform(scrollTo(), click(), replaceText("30.00"))
            screenshot("economic-editor-keyboard")
            onView(withId(R.id.editorSave)).perform(click())
            waitFor("programming created") { await(EconomicEvents.collection(uid).get(Source.SERVER)).documents.any { it.getString("title") == title } }
        }
        val doc = await(EconomicEvents.collection(uid).get(Source.SERVER)).documents.single { it.getString("title") == title }.reference
        ActivityScenario.launch<EconomicEventActivity>(Intent(context, EconomicEventActivity::class.java).putExtra("event_id", doc.id)).use { scenario ->
            waitFor("programming loaded") {
                var ready = false
                scenario.onActivity { ready = it.findViewById<android.view.View>(R.id.economicAmount).isEnabled }
                ready
            }
            onView(withId(R.id.economicAmount)).perform(scrollTo(), replaceText("35.00"), closeSoftKeyboard())
            onView(withId(R.id.editorSave)).perform(click())
            waitFor("programming edited") { await(doc.get(Source.SERVER)).getDouble("amount") == 35.0 }
        }
        ActivityScenario.launch<EconomicEventActivity>(Intent(context, EconomicEventActivity::class.java).putExtra("event_id", doc.id)).use { scenario ->
            waitFor("programming loaded for delete") {
                var ready = false
                scenario.onActivity { ready = it.findViewById<android.view.View>(R.id.economicAmount).isEnabled }
                ready
            }
            onView(withId(R.id.economicDelete)).perform(click())
            onView(withId(android.R.id.button1)).perform(click())
            waitFor("programming deleted") { !await(doc.get(Source.SERVER)).exists() }
        }
        assertEquals(0, await(db.collection("usuarios").document(uid).collection("movimientos").get(Source.SERVER)).size())
    }

    @Test fun calendarCreatesIncomeOnTheSelectedDayThroughVisibleActions() {
        val uid = auth.currentUser!!.uid
        val title = "Ingreso calendario QA ${System.nanoTime()}"
        val month = java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH, 1); add(java.util.Calendar.MONTH, 1) }
        val date = String.format(java.util.Locale.ROOT, "%04d-%02d-15", month.get(java.util.Calendar.YEAR), month.get(java.util.Calendar.MONTH) + 1)
        ActivityScenario.launch<NoticiasActivity>(Intent(context, NoticiasActivity::class.java)).use { scenario ->
            onView(withId(R.id.btnTabCalendar)).perform(click())
            onView(withId(R.id.btnNextMonth)).perform(scrollTo(), click())
            onView(withText("15")).perform(scrollTo(), click())
            onView(withId(R.id.btnAddEconomicEvent)).perform(click())
            onView(withId(R.id.economicDate)).perform(scrollTo()).check(matches(withText(date)))
            onView(withId(R.id.economicTitle)).perform(scrollTo(), replaceText(title), closeSoftKeyboard())
            onView(withId(R.id.economicAmount)).perform(scrollTo(), replaceText("12.50"), closeSoftKeyboard())
            onView(withId(R.id.economicType)).perform(scrollTo(), click())
            onView(withText(R.string.economic_income)).inRoot(isPlatformPopup()).perform(click())
            onView(withId(R.id.economicCategory)).perform(scrollTo(), click())
            onView(withText(com.example.coinly.data.MovementRepository.MovementRepository.INCOME_CATEGORIES.first())).inRoot(isPlatformPopup()).perform(click())
            screenshot("economic-editor")
            onView(withId(R.id.editorSave)).perform(click())
            waitFor("calendar income saved") {
                await(EconomicEvents.collection(uid).get(Source.SERVER)).documents.any { it.getString("title") == title && it.getString("date") == date && it.getString("type") == "income" }
            }
            waitFor("calendar returned") {
                var visible = false
                scenario.onActivity { visible = it.findViewById<android.view.View>(R.id.btnAddEconomicEvent).visibility == android.view.View.VISIBLE }
                visible
            }
            onView(withText(org.hamcrest.Matchers.containsString(title))).perform(scrollTo()).check(matches(isDisplayed()))
            screenshot("saved-calendar-event")
        }
    }
}
