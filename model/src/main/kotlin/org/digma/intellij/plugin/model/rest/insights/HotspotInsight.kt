package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

@JsonIgnoreProperties("updatedAt")
data class HotspotInsight

@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "score")
constructor(
    override val codeObjectId: String,
    val score: Int = 0
) : CodeObjectInsight {

    override val type: InsightType = InsightType.HotSpot
}