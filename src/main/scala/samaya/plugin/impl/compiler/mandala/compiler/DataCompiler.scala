package samaya.plugin.impl.compiler.mandala.compiler

import samaya.plugin.impl.compiler.mandala.MandalaParser
import samaya.plugin.impl.compiler.mandala.entry.TypeAlias
import samaya.structure.{Attribute, Constructor, DataDef, Field, Generic}
import samaya.structure.types.Permission.{Consume, Create, Inspect}
import samaya.structure.types.{Accessibility, Capability, Permission, SourceId, Type}

import scala.jdk.CollectionConverters._

trait DataCompiler extends CompilerToolbox {
  self: CapabilityCompiler with PermissionCompiler with ComponentBuilder with ComponentResolver=>

  override def visitTypeAliasDef(ctx: MandalaParser.TypeAliasDefContext): TypeAlias = {
    val sourceId = sourceIdFromContext(ctx)
    val name = visitName(ctx.name())
    val localGenerics = withDefaultCaps(genericFunCapsDefault){
      visitGenericArgs(ctx.genericArgs())
    }
    val typ = withGenerics(localGenerics) {
      visitTypeRef(ctx.typeRef())
    }
    registerTypeAlias(TypeAlias(name, localGenerics, typ, sourceId))
  }

  override def visitData(ctx: MandalaParser.DataContext): DataDef = {
    val localGenerics = withDefaultCaps(genericDataCapsDefault){
      visitGenericArgs(ctx.genericArgs())
    }

    withGenerics(localGenerics) {
      val loc = sourceIdFromContext(ctx)
      val access = withSupportedPerms(Set(Create, Consume, Inspect)){
        visitAccessibilities(ctx.accessibilities())
      }
      val dataName = visitName(ctx.name)

      //Do not change order
      registerDataDef(new DataDef {
        override val position: Int = nextPosition()
        override val index: Int = nextDataIndex()
        override val name: String = dataName
        override val attributes: Seq[Attribute] = Seq.empty
        override val accessibility: Map[Permission, Accessibility] = access
        override val generics: Seq[Generic] = localGenerics
        override val external: Option[Short] = visitExt(ctx.ext())
        override val capabilities: Set[Capability] = withDefaultCaps(dataCapsDefault){
          visitCapabilities(ctx.capabilities())
        }
        override val src: SourceId = loc
        override val constructors: Seq[Constructor] = withActiveAdt(this){
          visitCtrs(ctx.ctrs(),dataName)
        }
      })
    }
  }


  override def visitExt(ext: MandalaParser.ExtContext): Option[Short] = {
    //this spares the caller the check and simplifies the code
    if(ext == null) return None
    Some(ext.size.getText.toShort)
  }

  def visitCtrs(ctx: MandalaParser.CtrsContext, defaultName:String): Seq[Constructor] = {
    //this spares the caller the check and simplifies the code
    if(ctx == null) return Seq.empty
    if(ctx.fields() != null) {
      Seq(new Constructor {
        override val tag: Int = 0
        override val name: String = defaultName
        override val attributes: Seq[Attribute] = Seq.empty
        override val fields:Seq[Field] = {
          val fs = ctx.fields()
          val fields = if(fs == null) Seq.empty else fs.f.asScala
          withFreshIndex {
            fields.map(visitField).toSeq
          }
        }
        override val src: SourceId = sourceIdFromContext(ctx)
      })
    } else if(ctx.c != null && ctx.c.size() > 0){
      withFreshIndex{
        ctx.c.asScala.map(visitCtr).toSeq
      }
    } else {
      Seq(new Constructor {
        override val tag: Int = 0
        override val name: String = defaultName
        override val attributes: Seq[Attribute] = Seq.empty
        override val fields:Seq[Field] = Seq.empty
        override val src: SourceId = sourceIdFromContext(ctx)
      })
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
        fields.map(visitField).toSeq
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
