package com.characterfinder.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultsGroupAccessibilityTest {

    private lateinit var device: UiDevice

    @Before
    fun launchApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
        val context = instrumentation.targetContext
        val intent = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        assertTrue(device.wait(Until.hasObject(By.text("Buscar personajes")), TIMEOUT_MILLIS))
    }

    @Test
    fun resultsGroupExposesGroupLabelAndIndividualRowNodes() {
        val searchField = device.findObject(By.clazz("android.widget.EditText"))
        assertNotNull(searchField)
        searchField.text = "rick"

        assertTrue(
            device.wait(
                Until.hasObject(By.desc(Pattern.compile("Lista de resultados.*"))),
                RESULTS_TIMEOUT_MILLIS,
            ),
        )
        assertNotNull(device.findObject(By.desc(Pattern.compile("Nombre:.*"))))
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val RESULTS_TIMEOUT_MILLIS = 10_000L
    }
}
