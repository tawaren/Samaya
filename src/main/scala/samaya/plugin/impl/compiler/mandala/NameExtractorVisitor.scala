package samaya.plugin.impl.compiler.mandala

import org.antlr.v4.runtime.Token
import samaya.plugin.impl.compiler.mandala.MandalaParser.InstanceContext
import samaya.plugin.impl.compiler.mandala.components.instance.Instance

import scala.collection.JavaConverters._

//todo: add code id everywhere

class NameExtractorVisitor() extends MandalaBaseVisitor[Set[String]]{

  var componentName = ""
  override def visitComponent(ctx: MandalaParser.ComponentContext):Set[String] = {
    if(ctx.transaction() != null){
      return Set(getName(ctx.transaction().name()).getText)
    }
    if(ctx.module() != null){
      val main = getName(ctx.module().name()).getText
      val nested = visitModule(ctx.module())
      return nested + main
    }
    if(ctx.class_() != null){
      return Set(getName(ctx.class_().name()).getText)
    }
    if(ctx.topInstance() != null){
      return Set(getName(ctx.topInstance().instanceDef().asInstanceOf[InstanceContext].name()).getText)
    }
    Set.empty
  }

  override def visitInstance(ctx: InstanceContext): Set[String] = {
    Set(Instance.deriveTopName(componentName,getName(ctx.name()).getText))
  }

  override def defaultResult(): Set[String] = Set.empty

  override def aggregateResult(aggregate: Set[String], nextResult: Set[String]): Set[String] = {
    aggregate ++ nextResult
  }

  def getName(ctx: MandalaParser.NameContext): Token = {
    if(ctx.RAW_ID() != null) {
      ctx.RAW_ID().getSymbol
    } else {
      ctx.keywords().id
    }
  }
}
