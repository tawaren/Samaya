package samaya.validation

import samaya.structure.{CompiledModule, Constructor, DataDef, Field, FunctionSig, Generic, Indexed, Module, Package, Param, Result}
import samaya.structure.types.{Accessibility, Capability, Hash, Permission, SourceId}
import samaya.structure.types.Type.GenericType
import samaya.compilation.ErrorManager._
import samaya.types.Context



//todo: Modify so they use a Module Traverser not yet existing
//todo: Generate all these for non working compiler
//Note: this is designed for a working compiler
//       meaning it does not try to give good error messages if the compiler produces incorrect code
//       in case that still happens it just aborts
//        consequence: if an attacker manipulated a file after it was generated, the error provoked may be a bit cryptic
object ModuleValidator {
  private def processOrdered[E <: Indexed](seq:Seq[E], lookup:Int => Option[E], check: E => Unit, exact:Boolean = true):Unit = {
    var prevIndex = -1
    for(e <- seq) {
      if(!exact) {
        if(e.index <= prevIndex) feedback(PlainMessage("Sequence entries are not ordered", Error))
      } else {
        if(e.index != prevIndex+1) feedback(PlainMessage("Sequence entries are not strictly ordered", Error))
      }
      prevIndex = e.index
      if(!lookup(e.index).contains(e)) {
        unexpected("Entry inconsistent with lookup result")
      }
      check(e)
    }
  }

  def validateModule(module: Module, pkg:Package):Unit = {
    val context = Context(module,pkg)
    //check names are unique
    validateNamespace(module)
    //Check that their are no duplicates and that it is ordered and validate it
    processOrdered[FunctionSig](module.functions,module.function, f => validateFunction(f.src, f,context, "Function"), exact = false)
    processOrdered[FunctionSig](module.implements,module.implement, i => validateImplement(i.src, i,context), exact = false)
    processOrdered[FunctionSig](module.signatures,module.signature, s => validateFunction(s.src, s,context, "Signature", defineAllowed = true), exact = false)
    processOrdered[DataDef](module.dataTypes,module.dataType, d => validateDataType(d.src, d,context), exact = false)

  }

  def validateNamespace(module: Module):Unit = {
    var typNames = Set.empty[String]

    for(data <- module.dataTypes) {
      if(typNames.contains(data.name)) {
        feedback(LocatedMessage("type names in a module must be unique", data.src, Error))
      }
      typNames += data.name
    }
    for(sig <- module.signatures) {
      if(typNames.contains(sig.name)) {
        feedback(LocatedMessage("type names in a module must be unique", sig.src, Error))
      }
      typNames += sig.name
    }

    var callNames = Set.empty[String]
    for(fun <- module.functions) {
      if(callNames.contains(fun.name)) {
        feedback(LocatedMessage("function names in a module must be unique", fun.src, Error))
      }
      callNames += fun.name
    }
    for(impl <- module.implements) {
      if(callNames.contains(impl.name))  {
        feedback(LocatedMessage("function names in a module must be unique", impl.src, Error))
      }
      callNames += impl.name
    }
  }

  private def validateDataType(src:SourceId, data:DataDef, context: Context):Unit = {
    processOrdered[Generic](data.generics,data.generic, g => {})

    if(data.accessibility.keySet != Set(Permission.Consume, Permission.Inspect, Permission.Create)){
      feedback(LocatedMessage("DataTypes must have exactly the Inspect, Consume and Create permissions", src, Error))
    }

    val genericIdents:Set[String] = data.generics.map(p => p.name).toSet
    //check that generic names are unique
    if(genericIdents.size != data.generics.size){
      feedback(LocatedMessage("Data types generics must have unique names", src, Error))
    }

    processOrdered[Constructor](data.constructors,data.constructor, c => {
      processOrdered[Field](c.fields,c.field, field => {
        SignatureValidator.validateType(field.src, field.typ, data, context)
        SignatureValidator.validateTypeConstraints(field.typ, data.capabilities, data, context, promoteGenerics = true)
        field.typ match {
          case GenericType(_,pos) =>
            data.generic(pos) match {
              case Some(gd) => if(gd.phantom) {
                feedback(LocatedMessage("Phantoms can not be used as field types", gd.src, Error))
              }
              case None => feedback(LocatedMessage("Requested Generic does not exist", src, Error))
            }
          case _ =>
        }
      })

      val fieldIdents:Set[String] = c.fields.map(p => p.name).toSet
      //check that field names are unique
      if(fieldIdents.size != c.fields.size){
        feedback(LocatedMessage("Data types constructor field names must have unique names", src, Error))
      }

    })

    val ctrIdents:Set[String] = data.constructors.map(p => p.name).toSet
    //check that generic names are unique
    if(ctrIdents.size != data.constructors.size){
      feedback(LocatedMessage("Data types constructors must have unique names", src, Error))
    }

    if(data.external.nonEmpty && data.constructors.nonEmpty){
      feedback(LocatedMessage("External data types can not have constructors", src, Error))
    }

  }

  def validateImplement(src:SourceId, implement:FunctionSig, context: Context):Unit = {
    validateFunction(src,implement, context, "Implement")
    if(implement.results.size != 1) {
      feedback(LocatedMessage("Implements need to return single value of the implemented signature", src, Error))
    } else {
      processOrdered[Param](implement.params,implement.param, p => {
        val reqCaps = implement.result(0).get.typ.capabilities(context)
        if(implement.transactional && !p.typ.hasCap(context, Capability.Drop)) {
          feedback(LocatedMessage("Captured implement parameters for transactional signature must have a type with the Drop Capability", src, Error))
        }
        if(!reqCaps.subsetOf(p.typ.capabilities(context))){
          feedback(LocatedMessage("Captured implement parameters must have all the Capabilities declared on the implemented Signature", src, Error))
        }
      })
    }

  }

  //public so it can be shared with Transaction Validator
  def validateFunction(src:SourceId, function:FunctionSig, context: Context, name:String, defineAllowed:Boolean = false):Unit = {
    processOrdered[Generic](function.generics,function.generic, g => {})

    if(defineAllowed){
      if(function.accessibility.keySet != Set(Permission.Call, Permission.Define)){
        feedback(LocatedMessage(s"${name}s must have exactly the Call and Define permissions", src, Error))
      }
    } else {
      if(function.accessibility.keySet != Set(Permission.Call)){
        feedback(LocatedMessage(s"${name}s must have exactly the Call permission", src, Error))
      }
    }

    val genericIdents:Set[String] = function.generics.map(p => p.name).toSet

    //check that type param names are unique
    if(genericIdents.size != function.generics.size){
      feedback(LocatedMessage(s"$name generics must have unique names", src, Error))
    }

    processOrdered[Param](function.params,function.param, p => {
      SignatureValidator.validateType(p.src, p.typ, function, context)
      p.typ match {
        case GenericType(_, pos) =>
          function.generic(pos) match {
            case None => unexpected("Requested Generic does not exist")
            case Some(gf) =>  if(gf.phantom){
              feedback(LocatedMessage("Phantom generics not allowed as parameter type", gf.src, Error))
            }
          }
        case _ =>
      }
    })

    val paramIdents = function.params.map(p => p.name).toSet
    //check that param names are unique
    if(paramIdents.size != function.params.size){
      feedback(LocatedMessage(s"$name parameters must have unique names", src, Error))
    }

    processOrdered[Result](function.results,function.result, r => {
      SignatureValidator.validateType(r.src, r.typ, function, context)
    })

    val returnIdents = function.results.map(p => p.name).toSet
    //check that reutrn names are unique
    if(returnIdents.size != function.results.size){
      feedback(LocatedMessage(s"$name returns must have unique names", src, Error))
    }
  }
}
