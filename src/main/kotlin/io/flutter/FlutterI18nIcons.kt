package io.flutter
import com.intellij.openapi.util.IconLoader

import javax.swing.*

interface FlutterI18nIcons {
    companion object {

        val EmptyFlag = IconLoader.getIcon("/icons/flags/flag_empty.png", FlutterI18nIcons::class.java)             // 16x16
        val ArbFile = IconLoader.getIcon("/icons/arb_file.png", FlutterI18nIcons::class.java)                       // 16x16
        val ArbRefreshAction = IconLoader.getIcon("/icons/arb_refresh.png", FlutterI18nIcons::class.java)           // 16x16
    }

}
