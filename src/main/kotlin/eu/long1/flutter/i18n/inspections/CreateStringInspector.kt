package eu.long1.flutter.i18n.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ex.BaseLocalInspectionTool
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.DartReferenceExpression
import eu.long1.flutter.i18n.inspections.quickfix.CreateStringResourceQuickFix

class CreateStringInspector : BaseLocalInspectionTool() {
    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.fileType == DartFileType.INSTANCE) {
            val errorList = DartAnalysisServerService.getInstance(file.project).getErrors(file.virtualFile)
            val errors =
                errorList.filter { it.code == "undefined_getter" && it.message.contains("defined for the class 'S'.") }
            if (errors.isEmpty()) return null

            return errors.map {
                val string = PsiTreeUtil.findElementOfClassAtOffset(
                    file,
                    it.offset,
                    DartReferenceExpression::class.java,
                    false
                )!!
                val element = string.parent

                val quickFix = CreateStringResourceQuickFix(element, string.text)
                manager.createProblemDescriptor(
                    element,
                    quickFix.text,
                    isOnTheFly,
                    arrayOf(quickFix),
                    ProblemHighlightType.GENERIC_ERROR
                )
            }.toTypedArray()
        }

        return null
    }

    override fun getDisplayName(): String = "Create string resource from S class reference"
    override fun getGroupDisplayName(): String = "Flutter i18n"
}