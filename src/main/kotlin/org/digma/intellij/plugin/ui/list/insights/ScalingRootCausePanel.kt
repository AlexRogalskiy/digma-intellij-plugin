package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.editor.getCurrentPageNumberForInsight
import org.digma.intellij.plugin.editor.updateListOfEntriesToDisplay
import org.digma.intellij.plugin.model.rest.insights.EndpointInfo
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema
import org.digma.intellij.plugin.model.rest.insights.SpanScalingRootCauseInsight
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.list.openWorkspaceFileForSpan
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import javax.swing.*

private const val RECORDS_PER_PAGE_AFFECTED_ENDPOINTS = 3
fun spanScalingRootCauseItemsPanel(project: Project, insight: SpanScalingRootCauseInsight, moreData: HashMap<String, Any>): JPanel {

    val buttonToGraph = buildButtonToPercentilesGraph(project, insight.spanInfo.name,insight.spanInfo.instrumentationLibrary)

    val backwardsCompatibilityTitle = "Scaling Issue Root Cause";
    val backwardsCompatibilityDescription = "Significant performance degradation here";

    val uniqueInsightId = insight.codeObjectId + insight.type
    val lastPageNum: Int
    var rootCausePanel: DigmaResettablePanel? = null
    val paginationPanel = JPanel()
    val affectedEndpointsToDisplay = ArrayList<EndpointInfo>()
    val affectedEndpoints = insight.affectedEndpoints
    lastPageNum = affectedEndpoints.size

    rootCausePanel = object : DigmaResettablePanel() {
        override fun reset() {
            rebuildRootCauseAffectedEndpointRowsPanel(
                    rootCausePanel!!,
                    affectedEndpointsToDisplay,
                    project,
                    moreData
            )
            rebuildPaginationPanel(paginationPanel, lastPageNum,
                    affectedEndpoints, rootCausePanel, affectedEndpointsToDisplay, uniqueInsightId, RECORDS_PER_PAGE_AFFECTED_ENDPOINTS)
        }
    }

    updateListOfEntriesToDisplay(affectedEndpoints, affectedEndpointsToDisplay, getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum), RECORDS_PER_PAGE_AFFECTED_ENDPOINTS)
    buildRootCauseAffectedEndpointPanel(rootCausePanel, affectedEndpointsToDisplay, project, moreData)

    return createInsightPanel(
            project = project,
            insight = insight,
            title = backwardsCompatibilityTitle,
            description = backwardsCompatibilityDescription,
            iconsList = listOf(Laf.Icons.Insight.SCALE),
            bodyPanel = rootCausePanel,
            buttons = listOf(buttonToGraph),
            paginationComponent = buildPaginationRowPanel(lastPageNum, paginationPanel,
                    affectedEndpoints, rootCausePanel, affectedEndpointsToDisplay, uniqueInsightId, RECORDS_PER_PAGE_AFFECTED_ENDPOINTS)
    )
}

private fun rebuildRootCauseAffectedEndpointRowsPanel(
        rootCausePanel: DigmaResettablePanel,
        affectedEndpointToDisplay: List<EndpointInfo>,
        project: Project,
        moreData: HashMap<String, Any>
) {
    rootCausePanel.removeAll()
    buildRootCauseAffectedEndpointPanel(rootCausePanel, affectedEndpointToDisplay, project, moreData)
}

private fun buildRootCauseAffectedEndpointPanel(
        rootCausePanel: DigmaResettablePanel,
        affectedEndpointToDisplay: List<EndpointInfo>,
        project: Project,
        moreData: HashMap<String, Any>
) {
    rootCausePanel.layout = BoxLayout(rootCausePanel, BoxLayout.Y_AXIS)
    rootCausePanel.isOpaque = false

    val labelPanel = JPanel(BorderLayout())

    labelPanel.border = JBUI.Borders.empty()
    labelPanel.isOpaque = false

    val causedByLabel = JLabel(asHtml(spanGrayed("Affected Endpoints:")), SwingConstants.LEFT)

    causedByLabel.isOpaque = false
    labelPanel.add(causedByLabel)

    rootCausePanel.add(labelPanel)

    affectedEndpointToDisplay.forEach { affEndpoint: EndpointInfo ->
        rootCausePanel.add(getRootCauseAffectedEndpointPanel(project, moreData, affEndpoint))
    }
}

fun getRootCauseAffectedEndpointPanel(project: Project, moreData: HashMap<String, Any>, affectedEndpoint: EndpointInfo): JPanel{
    val endPointPanel = JPanel(BorderLayout())
    endPointPanel.border = JBUI.Borders.empty()
    endPointPanel.isOpaque = false

    val routeInfo = EndpointSchema.getRouteInfo(affectedEndpoint.route)
    var routeCodeObjectId = affectedEndpoint.codeObjectId;
    val shortRouteName =  routeInfo.shortName

    if (routeCodeObjectId != null && moreData.contains(routeCodeObjectId)) {
        val normalizedDisplayName = StringUtils.normalizeSpace(shortRouteName)
        val link = ActionLink(normalizedDisplayName) {
            openWorkspaceFileForSpan(project, moreData, routeCodeObjectId)
        }
        var targetClass = routeCodeObjectId?.substringBeforeLast("\$_\$");

        link.toolTipText = asHtml("$targetClass: $shortRouteName")
        endPointPanel.add(link, BorderLayout.NORTH)
    } else {
        val line1 = JBLabel(asHtml("${affectedEndpoint.serviceName}: <b>$shortRouteName</b>"))
        endPointPanel.add(line1)
    }
    return endPointPanel
}

private fun buildButtonToPercentilesGraph(project: Project, spanName: String,instLibrary: String): JButton {
    val analyticsService = AnalyticsService.getInstance(project)
    val button = ListItemActionButton("Histogram")
    button.addActionListener {
        val htmlContent = analyticsService.getHtmlGraphForSpanScaling(instLibrary, spanName, Laf.Colors.PLUGIN_BACKGROUND.getHex())
        HTMLEditorProvider.openEditor(project, "Scaling Graph of Span $spanName", htmlContent)
    }

    return button
}