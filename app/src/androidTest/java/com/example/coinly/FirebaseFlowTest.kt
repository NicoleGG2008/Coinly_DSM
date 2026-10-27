package com.example.coinly

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.coinly.data.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/** Runs only against the local Firebase Emulator Suite, never production. */
@RunWith(AndroidJUnit4::class)
class FirebaseFlowTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun until(description: String, check: () -> Boolean) {
        val end = System.currentTimeMillis() + 30000
        while (System.currentTimeMillis() < end) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if (check()) return
            Thread.sleep(150)
        }
        throw AssertionError("Timeout: $description; cloudError=${CloudStore.error}")
    }
    private fun shot(name: String) {
        Thread.sleep(1400)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val folder = File(app.getExternalFilesDir(null), "qa-captures").apply { mkdirs() }
        FileOutputStream(File(folder, "$name.png")).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "mkdir -p /sdcard/Download/coinly-qa").close()
        Thread.sleep(100)
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "cp ${File(folder, "$name.png").absolutePath} /sdcard/Download/coinly-qa/$name.png").use {
            java.io.FileInputStream(it.fileDescriptor).use { input -> input.readBytes() }
        }
    }
    private fun field(id: Int, value: String) {
        onView(withId(id)).perform(scrollTo(), replaceText(value), closeSoftKeyboard())
    }
    private fun nav(label: String) { onView(withContentDescription(label)).perform(click()) }
    private fun isScreen(target: Class<*>): Boolean {
        var visible=false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            visible=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).any { target.isInstance(it) }
        }
        return visible
    }
    private fun screen(target: Class<*>) {
        val intent = Intent(app, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<android.app.Activity>(intent)
    }
    @Test fun completeFirebaseFlow() {
        assertTrue("Build with -PfirebaseEmulator=true", BuildConfig.FIREBASE_EMULATOR)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { AuthSession.logout() }
        val unique = System.currentTimeMillis()
        val email = "qa-$unique@coinly.test"
        val password = "CoinlyQA123!"
        val nextPassword = "CoinlyQA456!"
        screen(MainActivity::class.java)
        until("protected screen redirects") { isScreen(LoginActivity::class.java) }
        field(R.id.etEmailLogin,"missing-$unique@coinly.test")
        field(R.id.etPasswordLogin,password)
        onView(withId(R.id.btnLogin)).perform(scrollTo(),click())
        until("invalid credentials rejected") {
            var enabled=false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val a=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).first()
                enabled=a.findViewById<android.view.View>(R.id.btnLogin)?.isEnabled == true
            }
            enabled && CloudStore.auth.currentUser==null
        }
        assertTrue(isScreen(LoginActivity::class.java))
        screen(RegisterActivity::class.java)
        field(R.id.etNameRegister, "Usuario QA")
        field(R.id.etEmailRegister, email)
        field(R.id.etPasswordRegister, password)
        field(R.id.etConfirmRegister, password)
        onView(withId(R.id.acCurrency)).perform(scrollTo(), click())
        onView(withText("USD")).perform(click())
        shot("02_registro")
        onView(withId(R.id.btnRegister)).perform(scrollTo(), click())
        until("registered and empty snapshot") { isScreen(MainActivity::class.java) && CloudStore.auth.currentUser != null && !CloudStore.loading && CloudStore.error == null }
        onView(withId(R.id.tvBalance)).check(matches(withText("$0.00")))
        assertTrue(CloudStore.movements.isEmpty()); assertTrue(CloudStore.budgets.isEmpty())
        shot("03_usuario_nuevo")
        val uid = CloudStore.auth.currentUser!!.uid

        // Actual movement UI persists the data to Firestore, including searchable tags.
        onView(withId(R.id.btnAddExpense)).perform(scrollTo(), click())
        field(R.id.etAmount, "10")
        onView(withId(R.id.acCategory)).perform(scrollTo(), click())
        onView(withText("Transporte")).perform(click())
        field(R.id.etDescription, "Pasaje QA")
        field(R.id.etTags, "bus, universidad")
        onView(withId(R.id.btnSheetSave)).perform(scrollTo(), click())
        until("movement stored") { CloudStore.movements.size == 1 && CloudStore.pending == 0 }
        assertEquals(10.0, Tasks.await(CloudStore.userRef().collection("movimientos").get(), 20, TimeUnit.SECONDS).documents.first().getDouble("amount")!!, 0.001)

        nav("Presupuestos")
        onView(withId(R.id.btnCreateBudgetAction)).perform(click())
        onView(withId(R.id.acBudgetCategory)).perform(scrollTo(), click())
        onView(withText("Transporte")).perform(click())
        field(R.id.etBudgetLimit, "100")
        onView(withId(R.id.btnBudgetSave)).perform(scrollTo(), click())
        until("budget stored and spent derived") {
            BudgetRepository(app).getBudgets().firstOrNull()?.spent == 10.0 && CloudStore.pending == 0
        }
        shot("04_presupuesto")
        nav("Movimientos")
        field(R.id.etSearch, "#universidad")
        onView(withId(R.id.tvMovementCount)).check(matches(withText("1 movimiento")))
        shot("05_busqueda_etiquetas")
        field(R.id.etSearch, "#sinresultado")
        onView(withId(R.id.tvMovementCount)).check(matches(withText("0 movimientos")))
        val monitor = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            monitor.getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).first().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        until("landscape") { app.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        onView(withId(R.id.etSearch)).check(matches(withText("#sinresultado")))
        onView(withId(R.id.tvMovementCount)).check(matches(withText("0 movimientos")))
        shot("06_horizontal_estado")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            monitor.getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).first().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        until("portrait") { app.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }

        // Verify edits and deletes change the budget total without a second stored balance.
        val movement=CloudStore.movements.first()
        Tasks.await(com.example.coinly.data.MovementRepository.MovementRepository(app).updateMovement(movement.copy(amount=25.0)),20,TimeUnit.SECONDS)
        until("updated total") { BudgetRepository(app).getBudgets().first().spent == 25.0 }
        Tasks.await(com.example.coinly.data.MovementRepository.MovementRepository(app).deleteMovement(movement.id),20,TimeUnit.SECONDS)
        until("deleted total") { BudgetRepository(app).getBudgets().first().spent == 0.0 }

        nav("Perfil")
        onView(withId(R.id.btnEditProfile)).perform(scrollTo(), click())
        field(R.id.etProfileName,"Perfil QA actualizado")
        field(R.id.etProfileUniversity,"Universidad QA")
        field(R.id.etProfileCareer,"Carrera QA")
        onView(withId(R.id.btnSaveProfile)).perform(scrollTo(),click())
        until("profile stored") { isScreen(PerfilActivity::class.java) && UserPrefs(app).name == "Perfil QA actualizado" && CloudStore.pending == 0 }
        shot("07_perfil")
        onView(withId(R.id.btnChangePassword)).perform(scrollTo(),click())
        field(R.id.etCurrentPassword,password)
        field(R.id.etNewPassword,nextPassword)
        field(R.id.etConfirmNewPassword,nextPassword)
        onView(withId(R.id.btnSavePassword)).perform(scrollTo(),click())
        until("password screen closed") {
            var closed=false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                closed=monitor.getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).none { it is ChangePasswordActivity }
            }
            closed
        }
        onView(withId(R.id.btnLogout)).perform(scrollTo(),click())
        until("signed out") { CloudStore.auth.currentUser == null }
        assertTrue(CloudStore.movements.isEmpty()); assertTrue(CloudStore.budgets.isEmpty())
        onView(withId(R.id.tvForgotPassword)).perform(click())
        field(R.id.etEmailForgot,email)
        onView(withId(R.id.btnSendReset)).perform(scrollTo(),click())
        until("reset shown") {
            var sent=false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val a=monitor.getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).first()
                sent=a.findViewById<android.view.View>(R.id.layoutForgotSent)?.visibility == android.view.View.VISIBLE
            }
            sent
        }
        shot("08_recuperacion_correo")

        // Second account gets an empty namespace, never the first user's data/profile.
        Tasks.await(CloudStore.auth.createUserWithEmailAndPassword("other-$unique@coinly.test",password),20,TimeUnit.SECONDS)
        until("second account empty") { !CloudStore.loading && CloudStore.movements.isEmpty() && CloudStore.budgets.isEmpty() }
        assertNotEquals(uid,CloudStore.auth.currentUser!!.uid)
        assertEquals("",UserPrefs(app).name)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { AuthSession.logout() }

        // Real login with the changed password and Recordar sesión.
        screen(LoginActivity::class.java)
        field(R.id.etEmailLogin,email)
        field(R.id.etPasswordLogin,nextPassword)
        onView(withId(R.id.cbRemember)).perform(scrollTo(),click())
        onView(withId(R.id.btnLogin)).perform(scrollTo(),click())
        until("login and remember") { isScreen(MainActivity::class.java) && CloudStore.auth.currentUser?.uid==uid && AuthSession.remember }
        until("first account budgets restored") { CloudStore.budgets.size==1 && !CloudStore.loading }
        assertEquals("Perfil QA actualizado",UserPrefs(app).name)
        shot("09_sesion_restaurada")
        Tasks.await(CloudStore.db.disableNetwork(),20,TimeUnit.SECONDS)
        try {
            until("offline state") { CloudStore.cached }
            assertEquals(1,CloudStore.budgets.size)
            shot("10_estado_sin_red")
        } finally {
            Tasks.await(CloudStore.db.enableNetwork(),20,TimeUnit.SECONDS)
        }
        until("connection recovered") { !CloudStore.cached && CloudStore.error==null }
    }
}
