package org.jetbrains.android.uipreview

import com.intellij.openapi.project.Project

import javax.swing.*

class DialogWrapper(project: Project?, private val panel: JComponent) : com.intellij.openapi.ui.DialogWrapper(project) {

    init {
        init()
    }

    override fun createCenterPanel(): JComponent = panel

}