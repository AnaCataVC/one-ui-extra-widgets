package com.anacatavc.oneuiextrawidgets

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Verifies that every widget preview layout only uses views annotated with `@RemoteView`
 * so Android launchers (such as One UI Home) can inflate `android:previewLayout` without
 * throwing `InflateException`.
 */
class WidgetPreviewLayoutTest {

    private val allowedRemoteViewTags = setOf(
        "LinearLayout",
        "FrameLayout",
        "RelativeLayout",
        "GridLayout",
        "ImageView",
        "TextView",
        "Button",
        "ImageButton",
        "ProgressBar",
        "AnalogClock",
        "Chronometer",
        "TextClock",
        "ViewStub"
    )

    @Test
    fun `all widget preview layouts use only RemoteViews-supported tags`() {
        val layoutDir = resolveResDir("layout")
        val previewFiles = layoutDir.listFiles { file ->
            file.isFile && file.name.startsWith("widget_preview_") && file.extension == "xml"
        }?.sortedBy { it.name }.orEmpty()

        assertTrue("Expected at least one widget_preview_*.xml file", previewFiles.isNotEmpty())

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val violations = mutableListOf<String>()

        for (file in previewFiles) {
            val doc = docBuilder.parse(file)
            collectDisallowedTags(doc.documentElement, file.name, violations)
        }

        assertTrue(
            "Found non-RemoteView tags in widget preview layouts:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    private fun collectDisallowedTags(element: Element, fileName: String, violations: MutableList<String>) {
        if (element.tagName !in allowedRemoteViewTags) {
            violations += "$fileName: <${element.tagName}> is not a supported @RemoteView"
        }
        val children = element.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element) {
                collectDisallowedTags(node, fileName, violations)
            }
        }
    }

    private fun resolveResDir(subDir: String): File {
        val candidates = listOf(
            File("src/main/res/$subDir"),
            File("app/src/main/res/$subDir")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate res/$subDir from ${File(".").absolutePath}")
    }
}
