package samaya.plugin.impl.compiler.mandala.compiler

import samaya.plugin.impl.compiler.mandala.{Environment, MandalaParser}

import scala.collection.JavaConverters._

//todo: add code id everywhere
//todo: enforce single return for single return opcodes in syntax
class MandalaBaseCompiler(override val env:Environment)
  extends ExpressionBuilder
    with ComponentBuilder
    with ClassBuilder
    with ModuleBuilder
    with InstanceBuilder
    with TransactionBuilder
    with ComponentResolver
    with PermissionCompiler
    with CapabilityCompiler
    with DataCompiler
    with SigCompiler
  {

  override def visitFile(ctx: MandalaParser.FileContext): Unit = {
    ctx.header().asScala.foreach(visitHeader)
    ctx.component().forEach(visitComponent)
  }

  override def visitComponent(ctx: MandalaParser.ComponentContext): Unit = {
    if(ctx.transaction() != null){
      visitTransaction(ctx.transaction())
    }
    if(ctx.module() != null){
      visitModule(ctx.module())
    }
    if(ctx.class_() != null){
      visitClass_(ctx.class_())
    }
    if(ctx.topInstance() != null){
      visitTopInstance(ctx.topInstance())
    }
  }
}
