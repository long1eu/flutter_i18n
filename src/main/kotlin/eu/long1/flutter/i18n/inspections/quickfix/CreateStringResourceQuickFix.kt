package eu.long1.flutter.i18n.inspections.quickfix

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import eu.long1.flutter.i18n.files.FileHelpers
import eu.long1.flutter.i18n.files.Syntax
import eu.long1.flutter.i18n.uipreview.DialogWrapper.Companion.showAndCreateFile

class CreateStringResourceQuickFix(element: PsiElement, private val fieldName: String) :
    LocalQuickFixOnPsiElement(element) {

    override fun getText(): String = "Create string value resource '$fieldName'"

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, psiFile: PsiFile, element: PsiElement, element2: PsiElement) {
        if(!FileHelpers.shouldActivateFor(project)) {
            return
        }

        val module = ModuleUtilCore.findModuleForFile(psiFile.virtualFile, project) ?: return

        WriteCommandAction.runWriteCommandAction(project, text, Syntax.GROUP_ID,
            Runnable {
                showAndCreateFile(project, module, fieldName, null, text)
            },
            psiFile
        )
    }
}