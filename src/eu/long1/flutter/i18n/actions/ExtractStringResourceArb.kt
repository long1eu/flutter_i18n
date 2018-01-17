package eu.long1.flutter.i18n.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import eu.long1.flutter.i18n.arb.ArbFileType
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

        val resDir = module.project.baseDir.findChild("res") ?:
                module.project.baseDir.createChildDirectory(this, "res")
        val valuesDir = resDir.findChild("values") ?:
                resDir.createChildDirectory(this, "values")

        ApplicationManager.getApplication().invokeLater {
            val panel = CreateArbResourcePanel(module, parent.name, parent.value?.text?.drop(1)?.dropLast(1) ?: "", valuesDir)
            val dialog = DialogWrapper(project, panel.panel)
            dialog.title = text
            dialog.showAndGet()

            if (dialog.isOK) {
                panel.selected.forEach {
                    val langFile = (psiManager.findFile(valuesDir.findChild(it)!!) as JsonFile?)!!
                    val jsonProperties = PsiTreeUtil.findChildrenOfAnyType(langFile, JsonProperty::class.java)
                    val exists = jsonProperties.any { it.name == panel.resId }

                    if (!exists) {
                        val buffer = StringBuilder(langFile.text.subSequence(0, langFile.textLength - 1))
                        if (jsonProperties.isNotEmpty()) buffer.append(",")
                        buffer.append("  \"${panel.resId}\": \"${panel.resValue}\"").append("}")
                        runWriteAction {
                            CommandProcessor.getInstance().executeCommand(project, {
                                documentManager.getDocument(langFile)!!.setText(buffer.toString())
                                CodeStyleManager.getInstance(psiManager).reformatText(langFile, 0, buffer.length)
                            }, "Override string in other languages", "Override string in other languages")
                        }
                    }
                }
            }
        }
    }
}