package org.digma.intellij.plugin.model.discovery

data class DocumentInfo(val path: String,
                        val methods: MutableMap<String, MethodInfo>){

   fun addMethodInfo(key: String,value: MethodInfo){
       methods.put(key,value)
   }
}