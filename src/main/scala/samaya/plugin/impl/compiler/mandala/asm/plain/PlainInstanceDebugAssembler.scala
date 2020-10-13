package samaya.plugin.impl.compiler.mandala.asm.plain

import java.io.{OutputStream, PrintStream}

import samaya.plugin.impl.compiler.mandala.components.instance.{DefInstance, Instance}
import samaya.plugin.service.{DebugAssembler, Selectors}
import samaya.structure.CompiledTransaction.TransactionFunctionDef
import samaya.structure.types._
import samaya.structure.{CompiledTransaction, Component, FunctionDef, FunctionSig, ImplementDef, Module, Package, Transaction, TypeParameterized}

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

      inst.applies
      printLine(s"PARAMS")
      indented{
        for(typ <- inst.applies) {
          printLine(s"PARAM: ${genTypeString(pkg, typ)}")
        }
      }
    }

    printLine("ALIASES")
    indented{
      for((name, bind) <- inst.funReferences) {
        val target = bind match {
          case Instance.RemoteEntryRef(module, offset) =>
            pkg.componentByLink(module).flatMap(_.asModule).flatMap(n => n.function(offset).map((n,_))) match {
              case Some((m,fun)) => m.name+"."+fun.name
              case None => bind.toString
            }
          case Instance.LocalEntryRef(offset) => s"error-local($offset)"
        }
        printLine(s"ALIAS: $name => $target")
      }
    }
    printer.close()

  }

  def genTypeString(pkg:Package, typ:Type):String = {
    def lookup(comp:Component, offset:Int) = typ match {
      case _: AdtType | _: LitType => comp match {
        case module: Module => module.dataType(offset)
        case _ => None
      }
      case _: SigType  => comp match {
        case module: Module => module.signature(offset)
        case _ => None
      }
    }

    typ match {
      case proj: Type.Projected => s"project ${genTypeString(pkg,proj.inner)}"
      case remote: Type.RemoteLookup[_] => s"${pkg.componentByLink(remote.moduleRef).flatMap(lookup(_,remote.offset)).map(_.name).getOrElse("?")}[${typ.applies.map(genTypeString(pkg,_)).reduceLeftOption(_ +", "+ _).getOrElse("")}]"
      case _  => "?"
    }
  }

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
