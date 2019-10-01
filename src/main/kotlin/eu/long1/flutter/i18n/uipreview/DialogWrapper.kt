package eu.long1.flutter.i18n.uipreview

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonPsiUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import eu.long1.flutter.i18n.files.FileHelpers
import javax.swing.JComponent

class DialogWrapper(project: Project?, private val panel: JComponent) : com.intellij.openapi.ui.DialogWrapper(project) {

    init {
        init()
    }

    override fun createCenterPanel(): JComponent = panel

    companion object {
        fun showAndCreateFile(
            project: Project,
            module: Module,
            resId: String?,
            value: String?,
            title: String,
            onFinish: (resId: String) -> Unit = {}
        ) {
            val panel = CreateArbResourcePanel(module, resId, value)
            val dialog = DialogWrapper(project, panel.panel)
            dialog.title = title

            val psiManager = PsiManager.getInstance(project)
            val valuesDir = FileHelpers.getValuesFolder(project)
            ApplicationManager.getApplication().invokeLater {
                dialog.showAndGet()

                val property = JsonElementGenerator(project).createProperty(panel.resId, "\"" + panel.resValue + "\"")
                if (dialog.isOK) {
                    WriteCommandAction.runWriteCommandAction(
                        project
                    ) {
                        panel.selected.forEach {
                            val file = valuesDir.findChild(it) ?: return@forEach
                            val langFile = psiManager.findFile(file) ?: return@forEach
                            PsiTreeUtil.getChildOfType(langFile, JsonObject::class.java) ?: run {
                                langFile.add(JsonElementGenerator(project).createObject("{}"))
                                PsiTreeUtil.getChildOfType(langFile, JsonObject::class.java)
                            }?.let { json ->
                                if (json.findProperty(panel.resId) == null)
                                    JsonPsiUtil.addProperty(json, property.copy() as JsonProperty, false)
                            }
                        }

                        onFinish(panel.resId)
                    }
                }
            }
        }
    }

}