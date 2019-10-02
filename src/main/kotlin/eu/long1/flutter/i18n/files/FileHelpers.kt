package eu.long1.flutter.i18n.files

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.flutter.pub.PubRoot
import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream
import com.intellij.openapi.application.ApplicationManager

object FileHelpers {
    @JvmStatic
    fun getResourceFolder(project: Project): VirtualFile {
        return project.baseDir.findChild("res")
            ?: project.baseDir.createChildDirectory(this, "res")
    }

    @JvmStatic
    fun getValuesFolder(project: Project): VirtualFile {
        val resFolder = getResourceFolder(project)
        return resFolder.findChild("values")
            ?: resFolder.createChildDirectory(this, "values")
    }

    @JvmStatic
    fun shouldActivateFor(project: Project): Boolean {
        PubRoot.forFile(project.projectFile)?.let { pubRoot ->
            FileInputStream(pubRoot.pubspec.path).use { inputStream ->
                (Yaml().load(inputStream) as? Map<String, Any>)?.let { map ->
                    (map["flutter_i18n"] as? Map<*, *>)?.let { pluginMap ->
                        // Did the user deactivate for this project?
                        if (isOptionFalse(pluginMap, "enable-flutter-i18n")) {
                            return@shouldActivateFor false;
                        }

                        // Automatically activated for Flutter projects.
                        return@shouldActivateFor if (pubRoot.declaresFlutter())
                            true
                        else
                            isOptionTrue(pluginMap, "enable-for-dart")
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

    @Suppress("SameParameterValue")
    private fun isOptionTrue(map: Map<*, *>, name: String): Boolean {
        val value = map[name]?.toString()?.toLowerCase()
        return "true" == value
    }

    @Suppress("SameParameterValue")
    private fun isOptionFalse(map: Map<*, *>, name: String): Boolean {
        val value = map[name]?.toString()?.toLowerCase()
        return "false" == value
    }
}