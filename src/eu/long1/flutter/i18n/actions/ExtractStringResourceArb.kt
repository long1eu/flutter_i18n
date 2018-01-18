package eu.long1.flutter.i18n.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonPsiUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import eu.long1.flutter.i18n.arb.ArbFileType
import eu.long1.flutter.i18n.files.FileHelpers
import org.jetbrains.android.uipreview.CreateArbResourcePanel
import org.jetbrains.android.uipreview.DialogWrapper

class ExtractStringResourceArb : PsiElementBaseIntentionAction(), HighPriorityAction {

    override fun getText(): String = "Override string in other languages"

    override fun getFamilyName(): String = text

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor?.document!!)
        val parent = PsiTreeUtil.getParentOfType(element, JsonProperty::class.java)

        return psiFile?.fileType == ArbFileType && parent != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val documentManager = PsiDocumentManager.getInstance(project)
        val psiManager = PsiManager.getInstance(project)

        val file = documentManager.getPsiFile(editor!!.document)!!.virtualFile
        val module = ModuleUtilCore.findModuleForFile(file, project)!!
        val parent = PsiTreeUtil.getParentOfType(element, JsonProperty::class.java) ?: return

        val valuesDir = FileHelpers.getValuesFolder(project)

        ApplicationManager.getApplication().invokeLater {
            val panel = CreateArbResourcePanel(module, parent.name, parent.value?.text?.drop(1)?.dropLast(1) ?: "", valuesDir)
            val dialog = DialogWrapper(project, panel.panel)
            dialog.title = text
            dialog.showAndGet()

            val property = JsonElementGenerator(project).createProperty(panel.resId, "\"${panel.resValue}\"")
            if (dialog.isOK) {
                WriteCommandAction.runWriteCommandAction(project) {
                    panel.selected.forEach {
                        val langFile = psiManager.findFile(valuesDir.findChild(it)!!)
                        val json = PsiTreeUtil.getChildOfType(langFile, JsonObject::class.java)!!
                        if (json.findProperty(panel.resId) == null)
                            JsonPsiUtil.addProperty(json, property.copy() as JsonProperty, false)
                    }
                }
            }
        }
    }
}