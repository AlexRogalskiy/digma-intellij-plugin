package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.discovery.CodeObjectInfo;
import org.digma.intellij.plugin.model.rest.insights.SpanInsight;
import org.digma.intellij.plugin.ui.model.insights.SpanGroupListViewItem;
import org.digma.intellij.plugin.ui.model.insights.SpanListViewItem;
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;

import java.util.function.Supplier;

public class SpanListViewItemBuilder extends ListViewItemBuilder {
    private final SpanInsight insight;
    private final CodeObjectInfo scope;

    public SpanListViewItemBuilder(SpanInsight insight, CodeObjectInfo scope) {
        this.insight = insight;
        this.scope = scope;
    }

    @Override
    public ListViewItem build(ListGroupManager groupManager) {

        var span = insight.getSpan();
        var groupKey = insight.getCodeObjectId() + "_" + span;
        SpanGroupListViewItem spanGroup = (SpanGroupListViewItem) groupManager.getOrCreateGroup(groupKey, () -> new SpanGroupListViewItem(span));

        spanGroup.setSortIndex(10);
        ListViewItem listViewItem = createListViewItem();
        spanGroup.getItems().add(listViewItem);
        return spanGroup;
    }

    private ListViewItem createListViewItem() {

        SpanListViewItem spanListViewItem = new SpanListViewItem(insight.getFlows());
        spanListViewItem.setSortIndex(0);
        spanListViewItem.setCodeObjectId(insight.getCodeObjectId());
        return spanListViewItem;
    }

    @Override
    public boolean accepted() {
        return true;
    }

}
