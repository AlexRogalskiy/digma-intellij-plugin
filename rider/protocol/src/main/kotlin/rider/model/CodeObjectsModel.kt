package rider.model

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.model.nova.ide.SolutionModel

//todo: create root, see ExampleModel
class CodeObjectsModel : Ext(SolutionModel.Solution) {


    private val RiderMethodInfo = structdef {
        field("id", PredefinedType.string)
        field("name", PredefinedType.string)
        field("displayName", PredefinedType.string)
        field("containingClass", PredefinedType.string)
        field("containingNamespace", PredefinedType.string)
        field("containingFile", PredefinedType.string)
        field("containingFileDisplayName", PredefinedType.string)
    }


    private val RiderCodeLensInfo = structdef {
        field("codeObjectId", PredefinedType.string)
        field("lensText", PredefinedType.string.nullable)
        field("lensTooltip", PredefinedType.string.nullable)
        field("moreText", PredefinedType.string.nullable)
        field("anchor", PredefinedType.string.nullable)
    }



    init {
        setting(CSharp50Generator.Namespace, "Digma.Rider.Protocol")
        setting(Kotlin11Generator.Namespace, "org.digma.rider.protocol")
        //todo: change to string and send the file to reanalyze
        signal("reanalyze", PredefinedType.string)
        signal("fileAnalyzed", PredefinedType.string)

        //key: document path, value: Document
        map("documents",
            PredefinedType.string,
            classdef("Document") {
                //map<codeObjectId,MethodInfo>
                map("methods", PredefinedType.string, RiderMethodInfo)
            }
        )

        map("codeLens",PredefinedType.string,RiderCodeLensInfo)

    }

}