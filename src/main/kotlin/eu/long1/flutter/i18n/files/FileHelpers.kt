package eu.long1.flutter.i18n.files

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.flutter.pub.PubRoot
import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction

object FileHelpers {
    @JvmStatic
    fun getResourceFolder(project: Project): VirtualFile =
        project.baseDir.findChild("res")
            ?: project.baseDir.createChildDirectory(this, "res")

    @JvmStatic
    fun getValuesFolder(project: Project): VirtualFile {
        return getResourceFolder(project).findChild("values")
            ?: getResourceFolder(project).createChildDirectory(this, "values")
    }

    @JvmStatic
    fun shouldActivateFor(project: Project): Boolean {
        PubRoot.forFile(project.projectFile)?.let { pubRoot ->
            FileInputStream(pubRoot.pubspec.path).use { inputStream ->
                (Yaml().load(inputStream) as? Map<String, Any>)?.let { map ->
                    (map["flutter_i18n"] as? Map<*, *>)?.let { pluginMap ->
                        // If activated for Dart, return true
                        if("true" == pluginMap["enable-for-dart"]?.toString()?.toLowerCase()) {
                            return true
                        }

                        // Only activated for Flutter projects.
                        if(!pubRoot.declaresFlutter()) {
                            return false
                        }

                        // Disabled for this Flutter project?
                        return "false" != pluginMap["enable-flutter-i18n"]?.toString()?.toLowerCase()
                    }
                }
            }
        }

        // Default is backward-compatible: to activate.
        return true
    }

    fun getI18nFile(project: Project, callback: (file: VirtualFile?) -> Unit) {
        ApplicationManager.getApplication().runWriteAction {
            PubRoot.forFile(project.projectFile)?.lib?.let { lib ->
                val generated = lib.findChild("generated")
                    ?: lib.createChildDirectory(this, "generated")

                callback(generated.findOrCreateChildData(this, "i18n.dart"))
            } ?: run {
                callback(null)
            }
        }
    }
}