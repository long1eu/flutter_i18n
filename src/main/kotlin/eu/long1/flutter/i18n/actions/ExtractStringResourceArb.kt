package eu.long1.flutter.i18n.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import eu.long1.flutter.i18n.arb.ArbFileType
import eu.long1.flutter.i18n.files.FileHelpers
import eu.long1.flutter.i18n.files.Syntax
import eu.long1.flutter.i18n.uipreview.DialogWrapper
import eu.long1.flutter.i18n.workers.Initializer

class ExtractStringResourceArb : PsiElementBaseIntentionAction(), HighPriorityAction {

    override fun getText(): String = "Override string in other languages"

    override fun getFamilyName(): String = text

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return editor?.let {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(it.document)
            val parent = PsiTreeUtil.getParentOfType(element, JsonProperty::class.java)

            psiFile?.fileType == ArbFileType && parent != null
        } ?: false
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if(!FileHelpers.shouldActivateFor(project)) {
            return
        }
        editor?.let {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(it.document) ?: return
            val module = ModuleUtilCore.findModuleForFile(psiFile.virtualFile, project) ?: return
            val parent = PsiTreeUtil.getParentOfType(element, JsonProperty::class.java) ?: return

            WriteCommandAction.runWriteCommandAction(
                project, text, Syntax.GROUP_ID,
                Runnable {
                    DialogWrapper.showAndCreateFile(
                        project,
                        module,
                        parent.name,
                        Initializer.getStringFromExpression(parent.value),
                        text
                    )
                }, psiFile
            )
        }
    }
}