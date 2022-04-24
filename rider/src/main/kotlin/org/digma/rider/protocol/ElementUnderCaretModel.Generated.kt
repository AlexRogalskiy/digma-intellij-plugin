@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.digma.rider.protocol

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [ElementUnderCaretModel.kt:8]
 */
class ElementUnderCaretModel private constructor(
    private val _elementUnderCaret: RdOptionalProperty<ElementUnderCaret>,
    private val _refresh: RdSignal<Unit>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(ElementUnderCaret)
        }
        
        
        
        
        const val serializationHash = 3452000726214409832L
        
    }
    override val serializersOwner: ISerializersOwner get() = ElementUnderCaretModel
    override val serializationHash: Long get() = ElementUnderCaretModel.serializationHash
    
    //fields
    val elementUnderCaret: IOptProperty<ElementUnderCaret> get() = _elementUnderCaret
    val refresh: ISignal<Unit> get() = _refresh
    //methods
    //initializer
    init {
        _elementUnderCaret.optimizeNested = true
    }
    
    init {
        bindableChildren.add("elementUnderCaret" to _elementUnderCaret)
        bindableChildren.add("refresh" to _refresh)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdOptionalProperty<ElementUnderCaret>(ElementUnderCaret),
        RdSignal<Unit>(FrameworkMarshallers.Void)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ElementUnderCaretModel (")
        printer.indent {
            print("elementUnderCaret = "); _elementUnderCaret.print(printer); println()
            print("refresh = "); _refresh.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): ElementUnderCaretModel   {
        return ElementUnderCaretModel(
            _elementUnderCaret.deepClonePolymorphic(),
            _refresh.deepClonePolymorphic()
        )
    }
    //contexts
}
val com.jetbrains.rd.ide.model.Solution.elementUnderCaretModel get() = getOrCreateExtension("elementUnderCaretModel", ::ElementUnderCaretModel)



/**
 * #### Generated from [ElementUnderCaretModel.kt:10]
 */
data class ElementUnderCaret (
    val fqn: String,
    val filePath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<ElementUnderCaret> {
        override val _type: KClass<ElementUnderCaret> = ElementUnderCaret::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ElementUnderCaret  {
            val fqn = buffer.readString()
            val filePath = buffer.readString()
            return ElementUnderCaret(fqn, filePath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ElementUnderCaret)  {
            buffer.writeString(value.fqn)
            buffer.writeString(value.filePath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as ElementUnderCaret
        
        if (fqn != other.fqn) return false
        if (filePath != other.filePath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + fqn.hashCode()
        __r = __r*31 + filePath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ElementUnderCaret (")
        printer.indent {
            print("fqn = "); fqn.print(printer); println()
            print("filePath = "); filePath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
