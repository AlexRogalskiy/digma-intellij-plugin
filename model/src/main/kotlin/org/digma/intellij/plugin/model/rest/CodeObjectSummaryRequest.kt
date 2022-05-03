package org.digma.intellij.plugin.model.rest

import com.fasterxml.jackson.annotation.JsonCreator

data class CodeObjectSummaryRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val environment: String, val codeObjectIds: List<String>)
