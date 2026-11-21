package com.characterfinder.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterGroupAccessibilityTest {

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
    fun filterGroupExposesGroupLabelAndIndividualChips() {
        assertTrue(
            device.wait(
                Until.hasObject(By.desc("Filtro de estado del personaje, 4 opciones")),
                TIMEOUT_MILLIS,
            ),
        )
        assertNotNull(device.findObject(By.text("Todos")))
        assertNotNull(device.findObject(By.text("Vivos")))
        assertNotNull(device.findObject(By.text("Muertos")))
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
