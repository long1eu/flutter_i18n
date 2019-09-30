package eu.long1.flutter.i18n.actions

import FlutterI18nIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import eu.long1.flutter.i18n.Log
import eu.long1.flutter.i18n.uipreview.DeviceConfiguratorPanel
import eu.long1.flutter.i18n.workers.I18nFileGenerator
import io.flutter.utils.FlutterModuleUtils
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

        if (dialog.isOK) {
            val locale = panel.editor.apply()
            val suffix = "${locale.language}${if (locale.country.isNotEmpty()) "_${locale.country}" else ""}"
            log.w(suffix)

            createFile(suffix, e.project!!)
        }
    }

    override fun update(e: AnActionEvent) {
        if (!FlutterModuleUtils.hasFlutterModule(e.project!!)) {
            e.presentation.isEnabled = false
            return
        }

        e.presentation.icon = FlutterI18nIcons.ArbFile
    }

    companion object {
        private val log = Log()

        fun createFile(suffix: String, project: Project) {
            val baseDir = project.baseDir
            val resFolder = baseDir.findChild("res") ?: baseDir.createChildDirectory(this, "res")
            val valuesFolder = resFolder.findChild("values") ?: resFolder.createChildDirectory(this, "values")
            val fileName = "strings_$suffix.arb"
            val editor = FileEditorManager.getInstance(project)

            valuesFolder.findChild(fileName)?.let { newVF ->
                editor.openFile(newVF, true)
            } ?: run {
                runWriteAction {
                    val newVF = valuesFolder.findOrCreateChildData(this, fileName)
                    PsiManager.getInstance(project).findFile(newVF)?.let { psiFile ->
                        PsiDocumentManager.getInstance(project).getDocument(psiFile)?.let { document ->
                            CommandProcessor.getInstance().executeCommand(project, {
                                document.setText("{}")
                            }, "Create new string file", "Create new string file")

                            PsiDocumentManager.getInstance(project).commitDocument(document)
                            document.addDocumentListener(object : DocumentListener {
                                override fun documentChanged(event: DocumentEvent) {
                                    ApplicationManager.getApplication().invokeLater(
                                        Runnable { I18nFileGenerator(project).generate() }, project.disposed
                                    )
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}
