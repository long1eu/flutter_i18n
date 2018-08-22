package eu.long1.flutter.i18n.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import eu.long1.flutter.i18n.files.Syntax
import eu.long1.flutter.i18n.uipreview.DialogWrapper.Companion.showAndCreateFile
import eu.long1.flutter.i18n.workers.Initializer
import io.flutter.utils.FlutterModuleUtils

class ExtractStringResourceDart : PsiElementBaseIntentionAction(), HighPriorityAction {

    override fun getText(): String = "Extract the string resource"

    override fun getFamilyName(): String = text

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (editor == null) return false
        val isFlutter = FlutterModuleUtils.isInFlutterModule(element)
        val dartExpression = PsiTreeUtil.getParentOfType(element, DartStringLiteralExpression::class.java)

        return isFlutter && dartExpression != null
    }


    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val docManager = PsiDocumentManager.getInstance(project)
        val psiFile = docManager.getPsiFile(editor.document)!!
        val module = ModuleUtilCore.findModuleForFile(psiFile.virtualFile, project)!!
        val dartExpression = PsiTreeUtil.getParentOfType(element, DartStringLiteralExpression::class.java)!!

        val resId = dartExpression.text
                ?.replace(Regex("[^A-Za-z0-9\\s]"), "")
                ?.toLowerCase()
                ?.split(" ")
                ?.take(10)
                ?.joinToString("_")

        runWriteCommandAction(project, text, Syntax.GROUP_ID, Runnable {
            showAndCreateFile(project, module, resId, Initializer.getStringFromExpression(dartExpression), text, {
                val fileText = dartExpression.text
                val newText = psiFile.text.replaceRange(dartExpression.textOffset, dartExpression.textOffset + fileText.length, "S.of(context).$it")
                editor.document.setText(newText)
            })
        }, psiFile)
    }
}