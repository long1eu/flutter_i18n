package eu.long1.flutter.i18n.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import FlutterI18nIcons
import eu.long1.flutter.i18n.Log
import org.jetbrains.android.uipreview.DeviceConfiguratorPanel
import javax.swing.JComponent


class NewArbFileAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val panel = DeviceConfiguratorPanel()
        val dialog = object : DialogWrapper(e.project!!) {

            init {
                init()
            }

            override fun createCenterPanel(): JComponent = panel
        }
        dialog.title = "Choose language"
        dialog.showAndGet()

        val locale = panel.editor.apply()
        val suffix = "${locale.language}${if (locale.country.isNotEmpty()) "_${locale.country}" else ""}"
        log.w(suffix)
        val baseDir = e.project!!.baseDir
        val resFolder = baseDir.findChild("res") ?: baseDir.createChildDirectory(this, "res")
        val valuesFolder = resFolder.findChild("values") ?: resFolder.createChildDirectory(this, "values")
        val fileName = "strings_$suffix.arb"
        var newVF = valuesFolder.findChild(fileName)

        val editor = FileEditorManager.getInstance(e.project!!)
        if (newVF == null) {
            runWriteAction {
                newVF = valuesFolder.findOrCreateChildData(this, fileName)
                val document = PsiDocumentManager.getInstance(e.project!!).getDocument(
                        PsiManager.getInstance(e.project!!).findFile(newVF!!)!!)!!

                document.setText("{}")
                PsiDocumentManager.getInstance(e.project!!).commitDocument(document)
            }
        } else {
            editor.openFile(newVF!!, true)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.icon = FlutterI18nIcons.ArbFile
    }

    companion object {
        private val log = Log()
    }
}
