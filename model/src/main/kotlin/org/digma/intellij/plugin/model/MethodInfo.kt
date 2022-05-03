package org.digma.intellij.plugin.model

data class MethodInfo(override val id: String,
                      val name: String,
                      val displayName: String,
                      val containingClass: String,
                      val containingNamespace: String,
                      val containingFile: String,
                      val containingFileDisplayName: String): CodeObjectInfo {

    override fun idWithType():String {
        return "method:$id"
    }
}