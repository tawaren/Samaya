package samaya.plugin.impl.compiler.mandala.compiler

import samaya.plugin.impl.compiler.simple.MandalaParser
import samaya.structure.{Attribute, Constructor, DataDef, Field, Generic}
import samaya.structure.types.Permission.{Consume, Create, Inspect}
import samaya.structure.types.{Accessibility, Capability, Permission, SourceId, Type}

import scala.collection.JavaConverters._

trait DataCompiler extends CompilerToolbox {
  self: CapabilityCompiler with PermissionCompiler with ComponentBuilder with ComponentResolver=>

  override def visitData(ctx: MandalaParser.DataContext): DataDef = {
    val localGenerics = withDefaultCaps(genericDataCapsDefault){
      visitGenericArgs(ctx.genericArgs())
    }

    withGenerics(localGenerics) {
      val loc = sourceIdFromContext(ctx)
      val access = withSupportedPerms(Set(Create, Consume, Inspect)){
        visitAccessibilities(ctx.accessibilities())
      }
      registerDataDef(new DataDef {
        override val position: Int = nextPosition()
        override val index: Int = nextDataIndex()
        override val name: String = visitName(ctx.name)
        override val attributes: Seq[Attribute] = Seq.empty
        override val accessibility: Map[Permission, Accessibility] = access
        override val generics: Seq[Generic] = localGenerics
        override val external: Option[Short] = visitExt(ctx.ext())
        override val top: Boolean = ctx.TOP() != null
        override val constructors: Seq[Constructor] = visitCtrs(ctx.ctrs())
        override val capabilities: Set[Capability] = withDefaultCaps(dataCapsDefault){
          visitCapabilities(ctx.capabilities())
        }
        override val src: SourceId = loc
      })
    }
  }


  override def visitExt(ext: MandalaParser.ExtContext): Option[Short] = {
    //this spares the caller the check and simplifies the code
    if(ext == null) return None
    Some(ext.size.getText.toShort)
  }

  override def visitCtrs(ctx: MandalaParser.CtrsContext): Seq[Constructor] = {
    //this spares the caller the check and simplifies the code
    if(ctx == null) return Seq.empty
    if(ctx.fields() != null) {
      Seq(new Constructor {
        override val tag: Int = 0
        override val name: String = "Ctr"
        override val attributes: Seq[Attribute] = Seq.empty
        override val fields:Seq[Field] = {
          val fs = ctx.fields()
          val fields = if(fs == null) Seq.empty else fs.f.asScala
          withFreshIndex {
            fields.map(visitField)
          }
        }
        override val src: SourceId = sourceIdFromContext(ctx)
      })
    } else {
      withFreshIndex{
        ctx.c.asScala.map(visitCtr)
      }
    }
  }

  override def visitCtr(ctx: MandalaParser.CtrContext): Constructor = new Constructor {
    override val tag: Int = nextIndex()
    override val name: String = visitName(ctx.name)
    override val attributes: Seq[Attribute] = Seq.empty
    override val fields:Seq[Field] = {
      val fs = ctx.fields()
      val fields = if(fs == null) Seq.empty else fs.f.asScala
      withFreshIndex{
        fields.map(visitField)
      }
    }
    override val src: SourceId = sourceIdFromContext(ctx)
  }

  override def visitField(ctx: MandalaParser.FieldContext): Field = new Field {
    override val name: String = visitName(ctx.name)
    override val pos: Int = nextIndex()
    override val typ: Type = visitTypeRef(ctx.typeRef())
    override val attributes: Seq[Attribute] = Seq.empty
    override val src: SourceId = sourceIdFromContext(ctx)
  }

}
