package org.digma.intellij.plugin.ui.common

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.launchBackground
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Alarm
import com.intellij.util.AlarmFactory
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.errors.IconButton
import org.digma.intellij.plugin.ui.list.insights.createDefaultBoxLayoutLineAxisPanelWithBackgroundWithFixedHeight
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.locks.ReentrantLock
import javax.swing.*


class NavigationPanel(
        project: Project,
        private val environmentsSupplier: EnvironmentsSupplier // assuming its a singleton
) : DigmaResettablePanel() {
    private val logger: Logger = Logger.getInstance(NavigationPanel::class.java)

    private val project: Project
    private val insightsModel: InsightsModel
    private val changeEnvAlarm: Alarm
    private val localHostname: String
    private val rebuildPanelLock = ReentrantLock()

    init {
        this.project = project
        this.insightsModel = InsightsViewService.getInstance(project).model
        changeEnvAlarm = AlarmFactory.getInstance().create()
        localHostname = CommonUtils.getLocalHostname()
        isOpaque = false
        layout = GridLayout(2, 1)
        border = JBUI.Borders.empty()

        rebuildInBackground()
    }


    override fun reset() {
        rebuildInBackground()
    }

    private fun rebuildInBackground() {
        val lifetimeDefinition = LifetimeDefinition()
        lifetimeDefinition.lifetime.launchBackground {
            rebuildPanelLock.lock()
            Log.log(logger::debug, "Lock acquired for rebuild Navigation panel process.")
            try {
                rebuild()
            } finally {
                rebuildPanelLock.unlock()
                Log.log(logger::debug, "Lock released for rebuild Navigation panel process.")
                lifetimeDefinition.terminate()
            }
        }
    }

    private fun rebuild() {
        ApplicationManager.getApplication().invokeLater {
            removeExistingComponentsIfPresent()
            buildNavigationPanelComponents()
            revalidate()
        }
    }

    private fun buildNavigationPanelComponents() {
        this.add(getFirstRowPanel())
        this.add(getSecondRowPanel())
    }

    private fun removeExistingComponentsIfPresent() {
        if (components.isNotEmpty()) {
            this.components.forEach {
                this.remove(it)
            }
        }
    }

    private fun getFirstRowPanel(): JPanel {
        val logoIconLabel = getLogoIconLabel()
        val comboBox = EnvironmentsDropdownPanel(project, insightsModel.getUsageStatus(), environmentsSupplier, localHostname)

        val parentPanel = JPanel(GridBagLayout())
        // Add the logo icon label to the parent panel with relative constraints
        val cLogo = GridBagConstraints()
        cLogo.gridx = 0
        cLogo.gridy = 0
        cLogo.anchor = GridBagConstraints.LINE_START
        parentPanel.add(logoIconLabel, cLogo)

        // Add the ComboBox to the parent panel with relative constraints
        val cComboBox = GridBagConstraints()
        cComboBox.gridx = 1
        cComboBox.gridy = 0
        cComboBox.weightx = 1.0
        cComboBox.anchor = GridBagConstraints.LINE_START
        cComboBox.fill = GridBagConstraints.HORIZONTAL
        parentPanel.add(comboBox, cComboBox)

//        rowPanel.add(getPointerButton()) // will be used later

        if (IDEUtilsService.getInstance(project).isJavaProject) {
            val cSettingsButton = GridBagConstraints()
            cSettingsButton.gridx = 2
            cSettingsButton.gridy = 0
            cSettingsButton.anchor = GridBagConstraints.LINE_START
            cSettingsButton.fill = GridBagConstraints.NONE
            parentPanel.add(getSettingsButton(), cSettingsButton)
        }

        parentPanel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, Laf.Colors.PLUGIN_BACKGROUND) // create 1px border in JetBrains dark gray color

        // Set the background color of the parent panel
        parentPanel.background = Laf.Colors.EDITOR_BACKGROUND

        // Set the preferred height of the parent panel based on the ComboBox
        val comboBoxSize = comboBox.preferredSize
        parentPanel.preferredSize = Dimension(parentPanel.width, comboBoxSize.height)

        return parentPanel
    }

    private fun getSecondRowPanel(): JPanel {
        val bgColor: Color = Laf.Colors.EDITOR_BACKGROUND

        val rowPanel = createDefaultBoxLayoutLineAxisPanelWithBackgroundWithFixedHeight(
                0, 5, 0, 5,
                bgColor,
                Laf.scalePanels(Laf.Sizes.PANEL_SIZE_32)
        )
//        rowPanel.add(getDashboardButton()) // will be used later
//        rowPanel.add(Box.createHorizontalGlue())
        rowPanel.add(ScopeLineResultPanel(project, insightsModel))
        rowPanel.add(Box.createHorizontalGlue())
//        rowPanel.add(getRelatedInsightsIconLabel()) // will be used later
        return rowPanel
    }

    private fun getLogoIconLabel(): JLabel {
        val logoIconLabel = JLabel(Laf.Icons.General.DIGMA_LOGO, SwingConstants.LEFT)
        logoIconLabel.horizontalAlignment = SwingConstants.LEFT
        logoIconLabel.verticalAlignment = SwingConstants.TOP
        logoIconLabel.isOpaque = false
        logoIconLabel.border = JBUI.Borders.empty(2, 13, 2, 10)
        return logoIconLabel
    }

    private fun getDashboardButton(): IconButton {
        val size = Laf.scalePanels(Laf.Sizes.BUTTON_SIZE_26)
        val buttonsSize = Dimension(size, size)
        val dashboardButton = IconButton(Laf.Icons.General.HOME)
        dashboardButton.preferredSize = buttonsSize
        dashboardButton.maximumSize = buttonsSize
        dashboardButton.border = JBUI.Borders.empty(2, 4)
        return dashboardButton
    }

    private fun getRelatedInsightsIconLabel(): JLabel {
        val logoIconLabel = JLabel(Laf.Icons.General.RELATED_INSIGHTS, SwingConstants.RIGHT)
        logoIconLabel.horizontalAlignment = SwingConstants.RIGHT
        logoIconLabel.verticalAlignment = SwingConstants.TOP
        logoIconLabel.isOpaque = false
        return logoIconLabel
    }

    private fun getPointerButton(): IconButton {
        val size = Laf.scalePanels(Laf.Sizes.BUTTON_SIZE_26)
        val buttonsSize = Dimension(size, size)
        val relatedInsightsButton = IconButton(Laf.Icons.General.POINTER)
        relatedInsightsButton.preferredSize = buttonsSize
        relatedInsightsButton.maximumSize = buttonsSize
        relatedInsightsButton.addActionListener {

        }
        return relatedInsightsButton
    }


    private fun getSettingsButton(): JPanel {
        val iconLabel = JLabel(Laf.Icons.Insight.THREE_DOTS, SwingConstants.RIGHT)
        iconLabel.horizontalAlignment = SwingConstants.RIGHT
        iconLabel.verticalAlignment = SwingConstants.TOP
        iconLabel.isOpaque = false
        iconLabel.border = JBUI.Borders.empty(2, 5)
        iconLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        iconLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                showSettingsMessage(
                    threeDotsIcon = iconLabel,
                    project = project
                )
            }
            override fun mouseExited(e: MouseEvent?) {}
            override fun mousePressed(e: MouseEvent?) {}
        })

        val wrapper = JPanel()
        wrapper.isOpaque = false
        wrapper.layout = FlowLayout(FlowLayout.CENTER, 10, 5)
        wrapper.add(iconLabel)
        return wrapper
    }

    private fun showSettingsMessage(threeDotsIcon: JLabel, project: Project) {
        HintManager.getInstance().showHint(SettingsHintPanel(project), RelativePoint.getSouthWestOf(threeDotsIcon), HintManager.HIDE_BY_ESCAPE, 5000)
    }
}