package samaya.plugin.impl.compiler.mandala.asm.plain

import java.io.{OutputStream, PrintStream}

import samaya.plugin.impl.compiler.mandala.components.instance.{DefInstance, Instance}
import samaya.plugin.impl.compiler.mandala.entry.SigImplement
import samaya.plugin.service.{DebugAssembler, Selectors}
import samaya.structure.types._
import samaya.structure.{Component, Package, TypeParameterized}
import samaya.types.Context

class PlainInstanceDebugAssembler extends DebugAssembler {

  override def matches(s: Selectors.DebugAssemblerSelector ): Boolean =  s match {
    case Selectors.DebugAssemblerSelector(_:Instance) => true
    case _ => false

  }

  override def serializeComponent(pkg:Package, cmp:Component, out:OutputStream): Unit = {
    cmp match {
      case inst: DefInstance => serializeInstance(pkg, inst, out)
      case _ =>
    }
  }

  private def serializeInstance(pkg:Package, inst: DefInstance, out: OutputStream): Unit = {
    val printer = new PrintStream(out)
    val ctx = new PrintContext(printer)
    import ctx._

    printLine(s"NAME: ${inst.name}")
    printLine(s"LANGUAGE: ${inst.language}")
    printLine(s"VERSION: ${inst.version}")
    printLine(s"CLASSIFIER: ${inst.classifier}")
    //    def attributes:Seq[ModuleAttribute]

    printLine(s"IMPLEMENTS")
    indented{
      val clazz = pkg.componentByLink(inst.classTarget) match {
        case Some(trg) => trg.name
        case None => inst.classTarget.toString
      }
      printLine(s"CLASS: $clazz")

      inst.classApplies
      printLine(s"PARAMS")
      indented{
        for(typ <- inst.classApplies) {
          printLine(s"PARAM: ${genTypeString(pkg, inst, typ)}")
        }
      }
    }

    printLine("IMPLEMENTS")
    indented{
      for(si@SigImplement(name,gens,fun,impl,_) <- inst.implements) {
        printLine(s"IMPLEMENT: $name");
        indented{
          printLine("GENERICS")
          indented{
            for(gen <- gens) {
              printLine(s"GENERIC: ${gen.name}")
              indented{
                //  def attributes:Seq[GenericAttribute]
                printLine(s"PHANTOM: ${gen.phantom}")
                printLine(s"CAPABILITIES: ${gen.capabilities.foldLeft("")(_ +" "+ _)}")
              }
            }
          }
          printLine(s"FUN_TARGET: ${genFuncString(pkg,si,fun)}")
          printLine(s"IMPL_TARGET: ${genFuncString(pkg,si,impl)}")
        }
      }
    }
    printer.close()

  }

  def genTypeString(pkg:Package, genCtx:TypeParameterized,typ:Type):String = typ.prettyString(Context(None,pkg), genCtx.generics.map(_.name))
  def genFuncString(pkg:Package, genCtx:TypeParameterized, func:Func):String = func.prettyString(Context(None,pkg), genCtx.generics.map(_.name))

  class PrintContext(out:PrintStream){
    var indent:Int = 0
    def printLine(line:String): Unit = {
      printIndent(out)
      out.println(line)
    }

    def printIndent(out:PrintStream): Unit = {
      for(_ <- 1 to indent){
        out.print("\t")
      }
    }

    def indented(body: => Unit){
      indent +=1
      body
      indent -=1
    }
  }
}
