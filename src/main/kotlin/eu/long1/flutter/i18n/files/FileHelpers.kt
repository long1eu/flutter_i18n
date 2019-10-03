package eu.long1.flutter.i18n.files

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.flutter.pub.PubRoot
import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream
import com.intellij.openapi.application.ApplicationManager
import io.flutter.utils.FlutterModuleUtils

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

    @Suppress("DuplicatedCode")
    @JvmStatic
    fun getPubSpecConfig(project: Project): PubSpecConfig? {
        PubRoot.forFile(project.projectFile)?.let { pubRoot ->
            FileInputStream(pubRoot.pubspec.path).use { inputStream ->
                (Yaml().load(inputStream) as? Map<String, Any>)?.let { map ->
                    return PubSpecConfig(project, pubRoot, map)
                }
            }
        }
        return null
    }

    @Suppress("DuplicatedCode")
    @JvmStatic
    fun shouldActivateFor(project: Project): Boolean = shouldActivateWith(getPubSpecConfig(project))

    @Suppress("DuplicatedCode")
    @JvmStatic
    fun shouldActivateWith(pubSpecConfig: PubSpecConfig?): Boolean {
        pubSpecConfig?.let {
            // Did the user deactivate for this project?
            if (it.isDisabled) {
                return@shouldActivateWith false
            }

            // Automatically activated for Flutter projects.
            return@shouldActivateWith if (it.pubRoot.declaresFlutter())
                true
            else
                it.isEnabledForDart
        }
        return false
    }
}

@Suppress("SameParameterValue")
private fun isOptionTrue(map: Map<*, *>?, name: String): Boolean {
    val value = map?.get(name)?.toString()?.toLowerCase()
    return "true" == value
}

@Suppress("SameParameterValue")
private fun isOptionFalse(map: Map<*, *>?, name: String): Boolean {
    val value = map?.get(name)?.toString()?.toLowerCase()
    return "false" == value
}

private const val PUBSPEC_KEY = "flutter_i18n"
private const val PUBSPEC_ENABLE_PLUGIN_KEY = "enable-flutter-i18n"
private const val PUBSPEC_DART_ENABLED_KEY = "enable-for-dart"

data class PubSpecConfig(
    val project: Project,
    val pubRoot: PubRoot,
    val map: Map<String, Any>,
    val i18nMap: Map<*, *>? = map[PUBSPEC_KEY] as? Map<*, *>,
    val isFlutterModule: Boolean = FlutterModuleUtils.hasFlutterModule(project),
    val isPluginConfigured: Boolean = i18nMap != null,
    val isEnabled: Boolean = isOptionTrue(i18nMap, PUBSPEC_ENABLE_PLUGIN_KEY),
    val isDisabled: Boolean = isOptionFalse(i18nMap, PUBSPEC_ENABLE_PLUGIN_KEY),
    val isEnabledForDart: Boolean = isOptionTrue(i18nMap, PUBSPEC_DART_ENABLED_KEY),
    val isDisabledForDart: Boolean = isOptionFalse(i18nMap, PUBSPEC_DART_ENABLED_KEY)
)
