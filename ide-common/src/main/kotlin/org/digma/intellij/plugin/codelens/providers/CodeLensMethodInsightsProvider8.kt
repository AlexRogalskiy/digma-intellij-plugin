package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class CodeLensMethodInsightsProvider8: DigmaCodeVisionProviderBase() {
    companion object {
        const val ID = "DigmaGenericProvider8"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 8"

    override val groupId: String
        get() = ID

}