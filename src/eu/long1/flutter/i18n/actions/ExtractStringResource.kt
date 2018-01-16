package eu.long1.flutter.i18n.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.*
import io.flutter.utils.FlutterModuleUtils
import org.jetbrains.android.uipreview.CreateArbResourcePanel
import org.jetbrains.android.uipreview.DialogWrapper

class ExtractStringResource : PsiElementBaseIntentionAction(), HighPriorityAction {

    override fun getText(): String = "Extract the string resource"

    override fun getFamilyName(): String = text

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (editor == null) return false
        val isFlutter = FlutterModuleUtils.isInFlutterModule(element)
        val methodDeclaration = PsiTreeUtil.getParentOfType(element, DartMethodDeclaration::class.java)
        val dartExpression = PsiTreeUtil.getParentOfType(element, DartStringLiteralExpression::class.java)
        val resId = getId(project, editor, element)

        return isFlutter && methodDeclaration != null && (dartExpression != null || resId != null)
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor!!.document)!!.virtualFile
        val module = ModuleUtilCore.findModuleForFile(file, project)!!
        val psiManager = PsiManager.getInstance(project)
        val dartExpression = PsiTreeUtil.getParentOfType(element, DartStringLiteralExpression::class.java)

        val resId = getId(project, editor, element) ?: dartExpression?.text
                ?.replace(Regex("[^A-Za-z0-9\\s]"), "")
                ?.toLowerCase()
                ?.split(" ")
                ?.take(10)
                ?.joinToString("_")

        println("id: $resId")

        val resDir = module.project.baseDir.findChild("res") ?:
                module.project.baseDir.createChildDirectory(this, "res")
        val valuesDir = resDir.findChild("values") ?:
                resDir.createChildDirectory(this, "values")

        val panel = CreateArbResourcePanel(module, resId ?: "", dartExpression?.text?.drop(1)?.dropLast(1) ?: "", valuesDir)
        val dialog = DialogWrapper(project, panel.panel)
        dialog.title = "Extract string resource"
        dialog.showAndGet()

        if (dialog.isOK && panel.selected.isNotEmpty()) {

            if (dartExpression != null) {
                val doc = (PsiDocumentManager.getInstance(project).getPsiFile(editor.document)!!)
                val fileText = dartExpression.text
                val newText = doc.text.replaceRange(dartExpression.textOffset,
                        dartExpression.textOffset + fileText.length,
                        "S.of(context).${panel.resId}")

                runWriteAction { editor.document.setText(newText) }
            }

            panel.selected.forEach {
                val langFile = (psiManager.findFile(valuesDir.findChild(it)!!) as JsonFile?)!!
                val jsonProperties = PsiTreeUtil.findChildrenOfAnyType(langFile, JsonProperty::class.java)
                val exists = jsonProperties.any { it.name == panel.resId }

                if (!exists) {
                    val buffer = StringBuilder(langFile.text.subSequence(0, langFile.textLength - 1))
                    if (jsonProperties.isNotEmpty()) buffer.append(",")
                    buffer.append("  \"${panel.resId}\": \"${panel.resValue}\"").append("}")
                    runWriteAction {
                        PsiDocumentManager.getInstance(project).getDocument(langFile)!!.setText(buffer.toString())
                        CodeStyleManager.getInstance(psiManager).reformatText(langFile, 0, buffer.length)
                    }
                }
            }
        }
    }


    @Suppress("UNCHECKED_CAST")
    private fun getId(project: Project, editor: Editor?, element: PsiElement): String? {
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor!!.document)!!
        val dartArgument = PsiTreeUtil.getParentOfType(element, DartArgumentList::class.java)
        val dartVar = PsiTreeUtil.getParentOfType(element, DartVarInit::class.java)
        val dartId = (if (dartArgument != null) dartArgument.children.first() else dartVar?.children?.first())?.text


        return when {
            dartArgument != null -> validateElement(project, dartArgument, DartArgumentList::class.java as Class<PsiElement>, file, dartId?.substringAfterLast("."))
            dartVar != null -> validateElement(project, dartVar, DartVarInit::class.java as Class<PsiElement>, file, dartId?.substringAfterLast("."))
            else -> null
        }
    }

    private fun validateElement(project: Project, element: DartPsiCompositeElement, clazz: Class<PsiElement>, file: PsiFile, dartId: String?): String? {
        DartAnalysisServerService.getInstance(project).getErrors(file.virtualFile).forEach {
            val errorElement: PsiElement? = PsiTreeUtil.findElementOfClassAtOffset(file, it.offset, clazz, false)
            if (errorElement != null && errorElement == element) {
                return if (element.text != null && element.text.contains("S.of(")) dartId else null
            }
        }

        return null
    }

}