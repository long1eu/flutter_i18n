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
            val analysisService = DartAnalysisServerService.getInstance(file.project).getErrors(file.virtualFile)

            val errors = analysisService.filter {
                it.code == "undefined_getter" && it.message.contains("defined for the class 'S'.")
            }

            if (errors.isNotEmpty()) {
                val problems = ArrayList<ProblemDescriptor>(errors.size)

                errors.forEach {
                    PsiTreeUtil.findElementOfClassAtOffset(
                        file,
                        it.offset,
                        DartReferenceExpression::class.java,
                        false
                    )?.let { string ->
                        val element = string.parent
                        val quickFix = CreateStringResourceQuickFix(element, string.text)

                        problems.add(manager.createProblemDescriptor(
                            element,
                            quickFix.text,
                            isOnTheFly,
                            arrayOf(quickFix),
                            ProblemHighlightType.GENERIC_ERROR
                        ))
                    }
                }

                if (problems.isNotEmpty()) {
                    return problems.toTypedArray()
                }
            }
        }

        return null
    }

    override fun getDisplayName(): String = "Create string resource from S class reference"
    override fun getGroupDisplayName(): String = "Flutter i18n"
}