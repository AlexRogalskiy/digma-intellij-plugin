package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

data class SpanFlow
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("percentage", "intermediateSpan", "lastServiceSpan", "firstService", "lastService")
constructor(val percentage: Float,
            var intermediateSpan: String?,
            val lastServiceSpan: String?,
            val firstService: Service?,
            val lastService: Service?) {


    data class Service
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @ConstructorProperties("service", "span")
    constructor(val service: String,
                val span: String)

}