package samaya.plugin.impl.asm.plain

import java.io.PrintStream
import java.io.OutputStream

import samaya.plugin.impl.inter.json.TransactionInterfaceImpl
import samaya.plugin.service.{DebugAssembler, Selectors}
import samaya.structure.CompiledTransaction.TransactionFunctionDef
import samaya.structure.{CompiledModule, CompiledTransaction, Component, FunctionDef, FunctionSig, ImplementDef, Interface, Module, ModuleInterface, Package, Transaction, TransactionInterface, TypeParameterized}
import samaya.structure.types.{AdtType, DefinedFunc, Func, Id, ImplFunc, LitType, OpCode, Ref, SigType, SourceId, StdFunc, Type, Val}
import samaya.types.Context

class PlainDebugAssembler extends DebugAssembler {

  override def matches(s: Selectors.DebugAssemblerSelector ): Boolean =  s match {
    case Selectors.DebugAssemblerSelector(_:Module) => true
    case Selectors.DebugAssemblerSelector(_:Transaction) => true
    case _ => false

  }

  override def serializeComponent(pkg:Package, cmp:Component, out:OutputStream): Unit = {
    cmp match {
      case module: Module => serializeModule(pkg, module, out)
      case transaction: Transaction => serializeTransaction(pkg, transaction, out)
      case _ =>
    }
  }

  private def serializeTransaction(pkg: Package, txt: Transaction, out: OutputStream): Unit = {
    val printer = new PrintStream(out)
    val ctx = new PrintContext(printer)
    import ctx._

    printLine(s"NAME: ${txt.name}")
    printLine(s"LANGUAGE: ${txt.language}")
    printLine(s"VERSION: ${txt.version}")
    printLine(s"CLASSIFIER: ${txt.classifier}")
    printLine("TRANSACTION")
    txt match {
      //Note we need to do the cast explicitly or it will use the wrong one
      case txt: CompiledTransaction => printFunction(ctx,pkg,new TransactionFunctionDef(txt),None)
      case _ => printFunction(ctx,pkg,txt,None)
    }

  }


  private def serializeModule(pkg:Package, module: Module, out: OutputStream): Unit = {
    val printer = new PrintStream(out)
    val ctx = new PrintContext(printer)
    import ctx._

    def whenDef(impl:FunctionSig)(body: ImplementDef => Unit) = impl match {
      case implDef: ImplementDef => body(implDef)
      case _ =>
    }

    printLine(s"NAME: ${module.name}")
    printLine(s"LANGUAGE: ${module.language}")
    printLine(s"VERSION: ${module.version}")
    printLine(s"CLASSIFIER: ${module.classifier}")
    //    def attributes:Seq[ModuleAttribute]

    printLine("DATA TYPES")
    indented{
      for(data <- module.dataTypes) {
        printLine(s"DATA TYPE: ${data.name}")
        indented{
          //        def attributes:Seq[DataTypeAttribute]

          printLine(s"EXTERNAL: ${data.external.nonEmpty}")
          if(data.external.nonEmpty) {
            indented { printLine(s"SIZE: ${data.external.get}")}
          }
          printLine("ACCESS")
          indented {
            for(acc <- data.accessibility) {
              printLine(s"${acc._1}: ${acc._2}")
            }
          }
          printLine(s"CAPABILITIES: ${data.capabilities.foldLeft("")(_ +" "+ _)}")
          printLine("GENERICS")
          indented{
            for(gen <- data.generics) {
              printLine(s"GENERIC: ${gen.name}")
              indented{
                //  def attributes:Seq[GenericAttribute]
                printLine(s"PHANTOM: ${gen.phantom}")
                printLine(s"CAPABILITIES: ${gen.capabilities.foldLeft("")(_ +" "+ _)}")
              }
            }
          }

          if(data.constructors.nonEmpty) {
              printLine("CONSTRUCTORS")
              indented{
                for(ctr <- data.constructors) {
                  printLine(s"CONSTRUCTOR: ${ctr.name}")
                  indented{
                    // def attributes:Seq[ConstructorAttribute]
                    printLine(s"FIELDS")
                    indented {
                      for (field <- ctr.fields) {
                        printLine(s"FIELD: ${field.name}")
                        indented {
                          printType(ctx,pkg,module,data,field.typ)
                          //def attributes:List[FieldAttribute]
                        }
                      }
                    }
                  }
                }
              }
          }
        }
      }
    }

    printLine("SIGNATURES")
    indented{
      for(sig <- module.signatures) {
        printLine(s"SIGNATURE: ${sig.name}")
        indented{
          printLine("ACCESS")
          indented {
            for(acc <- sig.accessibility) {
              printLine(s"${acc._1}: ${acc._2}")
            }
          }
          printLine(s"TRANSACTIONAL: ${sig.transactional}")
          printLine(s"CAPABILITIES: ${sig.capabilities.foldLeft("")(_ +" "+ _)}")
          printLine("GENERICS")
          indented{
            for(gen <- sig.generics) {
              printLine(s"GENERIC: ${gen.name}")
              indented{
                //  def attributes:Seq[GenericAttribute]
                printLine(s"PHANTOM: ${gen.phantom}")
                printLine(s"CAPABILITIES: ${gen.capabilities.foldLeft("")(_ +" "+ _)}")
              }
            }
          }

          printLine("PARAMS")
          indented{
            newScope(sig.src)
            for(param <- sig.params) {
              printLine(s"PARAM: ${param.name}")
              indented{
                printType(ctx,pkg,module,sig,param.typ)
                printLine(s"CONSUME: ${param.consumes}")
                //def attributes:Seq[ParamAttribute]
              }
            }
          }

          printLine("RETURNS")
          indented{
            for(ret <- sig.results) {
              printLine(s"RETURN: ${ret.name}")
              indented{
                printType(ctx,pkg,module,sig,ret.typ)
                //def attributes:Seq[ParamAttribute]
              }
            }
          }
        }
      }
    }

    printLine("FUNCTIONS")
    indented{
      for(fun <- module.functions) {
        printFunction(ctx,pkg,fun, Some(module))
      }
    }

    printLine("IMPLEMENTS")
    indented{
      for(impl <- module.implements) {
        printLine(s"IMPLEMENT: ${impl.name}")
        indented{
          printLine(s"TRANSACTIONAL: ${impl.transactional}")
          whenDef(impl){ impl =>
            printLine(s"EXTERNAL: ${impl.external}")
            printLine(s"DEFINES: ${genTypeString(pkg, module, impl, impl.implements)}")
          }

          printLine("ACCESS")
          indented {
            for(acc <- impl.accessibility) {
              printLine(s"${acc._1}: ${acc._2}")
            }
          }
          printLine("GENERICS")
          indented{
            for(gen <- impl.generics) {
              printLine(s"GENERIC: ${gen.name}")
              indented{
                //  def attributes:Seq[GenericAttribute]
                printLine(s"PHANTOM: ${gen.phantom}")
                printLine(s"CAPABILITIES: ${gen.capabilities.foldLeft("")(_ +" "+ _)}")
              }
            }
          }

          printLine("PARAMS")
          indented{
            newScope(impl.src)
            for(param <- impl.params) {
              printLine(s"PARAM: ${param.name}")
              indented{
                printType(ctx,pkg,module,impl,param.typ)
                printLine(s"CONSUME: ${param.consumes}")
                //def attributes:Seq[ParamAttribute]
              }
            }
          }

          printLine("RETURNS")
          indented{
            for(ret <- impl.results) {
              printLine(s"RETURN: ${ret.name}")
              indented{
                printType(ctx,pkg,module,impl,ret.typ)
                //def attributes:Seq[ParamAttribute]
              }
            }
          }

          whenDef(impl){ impl =>
            printLine("BINDINGS")
            indented {
              printLine("PARAMS")
              indented{
                for(param <- impl.sigParamBindings) {
                  printLine(s"BINDING: ${param.name}")
                }
              }
              printLine("RETURNS")
              indented{
                for(ret <- impl.sigResultBindings) {
                  printLine(s"BINDING: ${ret.name}")
                }
              }
            }

            if(impl.code.nonEmpty){
              printLine("CODE")
              indented{ printCode(ctx, pkg, module, impl, impl.code) }
            }
          }
        }
      }
    }
    printer.close()

  }

  private def printFunction(ctx:PrintContext, pkg:Package, fun:FunctionSig, module: Option[Module]): Unit = {
    import ctx._

    def whenDef(body: FunctionDef => Unit) = fun match {
      case functionDef: FunctionDef => body(functionDef)
      case _ =>
    }

    printLine(s"FUNCTION: ${fun.name}")
    indented{
      whenDef(fun => printLine(s"EXTERNAL: ${fun.external}"))
      printLine(s"TRANSACTIONAL: ${fun.transactional}")
      printLine("ACCESS")
      indented {
        for(acc <- fun.accessibility) {
          printLine(s"${acc._1}: ${acc._2}")
        }
      }
      printLine("GENERICS")
      indented{
        for(gen <- fun.generics) {
          printLine(s"GENERIC: ${gen.name}")
          indented{
            //  def attributes:Seq[GenericAttribute]
            printLine(s"PHANTOM: ${gen.phantom}")
            printLine(s"CAPABILITIES: ${gen.capabilities.foldLeft("")(_ +" "+ _)}")
          }
        }
      }

      printLine("PARAMS")
      indented{
        newScope(fun.src)
        for(param <- fun.params) {
          printLine(s"PARAM: ${param.name}")
          indented{
            printType(ctx,pkg,module,fun,param.typ)
            printLine(s"CONSUME: ${param.consumes}")
            //def attributes:Seq[ParamAttribute]
          }
        }
      }

      printLine("RETURNS")
      indented{
        for(ret <- fun.results) {
          printLine(s"RETURN: ${ret.name}")
          indented{
            printType(ctx,pkg,module,fun,ret.typ)
            //def attributes:Seq[ParamAttribute]
          }
        }
      }
      whenDef(fun => if(fun.code.nonEmpty){
        printLine("CODE")
        indented{ printCode(ctx, pkg, module, fun, fun.code) }
      })
    }
  }

  private def printCode(ctx:PrintContext, pkg:Package, module:Module, genCtx:TypeParameterized, code:Seq[OpCode]): Unit = printCode(ctx,pkg,Some(module), genCtx, code)
  private def printCode(ctx:PrintContext, pkg:Package, module:Option[Module], genCtx:TypeParameterized, code:Seq[OpCode]): Unit = {
    for(op <- code) printOpCode(ctx, pkg, module, genCtx, op)
  }

  private def printOpCode(ctx:PrintContext, pkg:Package, module:Module, genCtx:TypeParameterized, op:OpCode): Unit = printOpCode(ctx,pkg, Some(module), genCtx,op)
  private def printOpCode(ctx:PrintContext, pkg:Package, module:Option[Module], genCtx:TypeParameterized, op:OpCode): Unit = {
    import ctx._
    op match {
      case OpCode.Lit(res, value, _) =>  printLine(assoc"[${op.id}] ${res.id.name} = lit ${value.bytes.map((b:Byte) => String.format("%02X", Byte.box(b))).foldLeft("0x")(_+_)}")
      case OpCode.Let(res, block, _) =>
        printLine(assoc"[${op.id}] ${res.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")} = let:")
        indented{ printCode(ctx,pkg,module,genCtx,block) }
      case OpCode.Fetch(res, src, mode, _) => printLine(assoc"[${op.id}] ${res.name} = fetch($mode) ${ref(src)}")
      case OpCode.Return(res, src, _) => printLine(assoc"[${op.id}] ${res.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")}  = return (${src.map(ref).reduceLeftOption(_+", "+_).getOrElse("")})")
      case OpCode.Discard(trg, _) => printLine(s"discard ${ref(trg)}")
      case OpCode.DiscardMany(trg, _) => printLine(s"discard ${trg.map(ref).reduce(_+", "+_)}")
      case OpCode.Unpack(res, src, mode, _) => printLine(assoc"[${op.id}] ${res.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")} = unpack($mode) ${ref(src)}")
      case OpCode.InspectUnpack(res, src, _) => printLine(assoc"[${op.id}] ${res.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")} = inpect ${ref(src)}")
      case OpCode.Field(res, src, pos, mode, _) => printLine(assoc"[${op.id}] ${res.name} = field#${pos.name}($mode) ${ref(src)}")
      case OpCode.Switch(res, src, branches, mode, _) =>
        printLine(assoc"[${op.id}] ${res.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")} = switch($mode) ${ref(src)}:")
        indented{
          for((ctrName,(ids,block)) <- branches) {
            printLine(s"case ${ctrName.name}(${ids.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")})")
            indented{ printCode(ctx,pkg,module,genCtx,block) }
          }
        }
      case OpCode.InspectSwitch(res, src, branches, _) =>
        printLine(assoc"[${op.id}] ${res.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")} = inspect ${ref(src)}:")
        indented{
          for((ctrName,(ids,block)) <- branches) {
            printLine(s"case ${ctrName.name}(${ids.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")})")
            indented{ printCode(ctx,pkg,module,genCtx,block) }
          }
        }
      case OpCode.Pack(res, src, ctr, mode, _) => printLine(assoc"[${op.id}] ${res.id.name} = pack($mode)#${genTypeString(pkg,module,genCtx,res.typ)}|${ctr.name}(${src.map(ref).reduceLeftOption(_+", "+_).getOrElse("")})")
      case OpCode.Invoke(res, func, param, _) => printLine(assoc"[${op.id}] ${res.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")} = call#${genFuncString(pkg,module,genCtx,func)}(${param.map(ref).reduceLeftOption(_+", "+_).getOrElse("")})")
      case OpCode.InvokeSig(res, src, param, _) => printLine(assoc"[${op.id}] ${res.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")} = sig call#${ref(src)}(${param.map(ref).reduceLeftOption(_+", "+_).getOrElse("")})")
      case OpCode.TryInvoke(res, func, param, succ, fail, _) =>
        printLine(assoc"[${op.id}] ${res.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")} = try call#${genFuncString(pkg,module,genCtx,func)}(${param.map(p => (if(p._1) "essential " else "") + ref(p._2)).reduceLeftOption(_+", "+_).getOrElse("")}):")
        indented{
          printLine(s"success(${succ._1.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")})")
           indented{ printCode(ctx,pkg,module,genCtx,succ._2) }
          printLine(s"fail(${fail._1.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")})")
          indented{ printCode(ctx,pkg,module,genCtx,fail._2) }
        }
      case OpCode.TryInvokeSig(res, src, param, succ, fail, _) =>
        printLine(assoc"[${op.id}] ${res.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")} = sig call#${ref(src)}(${param.map(p => (if(p._1) "essential " else "") + ref(p._2)).reduceLeftOption(_+", "+_).getOrElse("")}):")
        indented{
          printLine(s"success(${succ._1.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")})")
          indented{ printCode(ctx,pkg,module,genCtx,succ._2) }
          printLine(s"fail(${fail._1.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")})")
          indented{ printCode(ctx,pkg,module,genCtx,fail._2) }
        }
      case OpCode.Project(res, src, _) => printLine(assoc"[${op.id}] ${res.name} = project ${ref(src)}")
      case OpCode.UnProject(res, src, _) => printLine(assoc"[${op.id}] ${res.name} = unproject${ref(src)}")
      case OpCode.RollBack(res, src, types, _) => printLine(assoc"[${op.id}] ${res.map(_.name).reduceLeftOption(_+", "+_).getOrElse("")} = rollback(${src.map(ref).reduceLeftOption(_+", "+_).getOrElse("")}):(${types.map(genTypeString(pkg,module,genCtx,_)).reduceLeftOption(_+", "+_).getOrElse("")})")
      case _:OpCode.VirtualOpcode => printLine(s"virtual (should have been eliminated)")
    }
  }

  def genFuncString(pkg:Package, module:Module, genCtx:TypeParameterized, func:Func):String = genFuncString(pkg, Some(module), genCtx, func)
  def genFuncString(pkg:Package, module:Option[Module], genCtx:TypeParameterized, func:Func):String = func.prettyString(Context(module,pkg), genCtx.generics.map(_.name))

  def genTypeString(pkg:Package, module:Module, genCtx:TypeParameterized,typ:Type):String = genTypeString(pkg,Some(module),genCtx,typ)
  def genTypeString(pkg:Package, module:Option[Module], genCtx:TypeParameterized,typ:Type):String = typ.prettyString(Context(module,pkg), genCtx.generics.map(_.name))

  private def printType(ctx:PrintContext, pkg:Package, module:Module, genCtx:TypeParameterized,  typ:Type): Unit = printType(ctx, pkg, Some(module), genCtx, typ)
  private def printType(ctx:PrintContext, pkg:Package, module:Option[Module], genCtx:TypeParameterized,  typ:Type): Unit = {
    ctx.printLine(s"TYPE: ${genTypeString(pkg,module,genCtx,typ)}")
  }

  class PrintContext(out:PrintStream){
    var indent:Int = 0
    var assocs:Map[SourceId,String] = Map()

    def assigAssoc(s:Any):Any = {
      s match {
        case id: SourceId =>
          assocs.get(id) match {
            case Some(value) => value
            case None =>
              val no = assocs.size
              assocs = assocs.updated(id, no.toString)
              no
          }
        case other => other
      }
    }

    def ref(r:Ref):String = {
      r match {
        case v:Val => s"${v.id.name}@${assocs.getOrElse(v.src,"_")}"
        case id:Id => s"${id.name}"
      }
    }

    implicit class AssocHelper(val sc: StringContext) {
      def assoc(args: Any*): String = sc.s(args.map(assigAssoc):_*)

    }

    def printLine(line:String): Unit = {
      printIndent(out)
      out.println(line)
    }

    def printIndent(out:PrintStream): Unit = {
      for(_ <- 1 to indent){
        out.print("\t")
      }
    }

    def newScope(fId:SourceId): Unit = {
      assocs = Map(fId -> "param")
    }

    def indented(body: => Unit): Unit = {
      indent +=1
      body
      indent -=1
    }

  }

}
