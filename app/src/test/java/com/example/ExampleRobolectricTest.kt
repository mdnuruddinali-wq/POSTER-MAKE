package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CanvasFormat
import com.example.data.model.CanvasLayer
import com.example.data.model.DesignProject
import com.example.data.model.LayerType
import com.example.data.model.PosterFont
import com.example.data.model.TemplateCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PosterStudio", appName)
  }

  @Test
  fun `project create map omits DocumentId and null fields`() {
    val layer = CanvasLayer(
      id = "l1",
      type = LayerType.TEXT,
      text = "TEST",
      font = PosterFont.SPACE_GROTESK
    )
    val project = DesignProject(
      id = "proj_abc",
      title = "Launch Poster",
      aspectRatio = CanvasFormat.POSTER_3_4.id,
      templateResKey = null,
      localImagePath = null,
      layers = listOf(layer.toMap())
    )
    val payload = project.toCreateMap("user_123")
    assertFalse(payload.containsKey("id"))
    assertFalse(payload.containsKey("templateResKey"))
    assertFalse(payload.containsKey("localImagePath"))
    assertEquals("user_123", payload["userId"])
    assertEquals("Launch Poster", payload["title"])
    assertTrue(TemplateCatalog.templates.isNotEmpty())
  }
}
