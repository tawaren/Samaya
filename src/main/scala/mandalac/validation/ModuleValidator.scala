package mandalac.validation


import mandalac.registries.ModuleRegistry
import mandalac.structure.types
import mandalac.structure.types.{Capability, Hash, Type, Visibility}
import mandalac.structure.types.Type.{GenericType, ImageType, LocalType, NativeType, RealType}
import mandalac.structure.{Constructor, DataType, Field, FunctionDef, Generic, Indexed, Module, Param, Result, Risk, TypeParameterized}
import mandalac.compilation.ErrorHandler._


//todo: Better doc
//Note: this is designed for a working compiler
//       meaning it does not try to give good error messages if the compiler produces incorrect code
//       in case that still happens it just aborts
//        consequence: if an attacker manipulated a file after it was generated, the error provoked may be a bit cryptic
object ModuleValidator {


  private def processOrdered[E <: Indexed](seq:Seq[E], lookup:Int => Option[E], check: E => Unit, exact:Boolean = true):Unit = {
    var prevIndex = -1
    for(e <- seq) {
      if(!exact) {
        if(e.index <= prevIndex) unexpected("Sequence entries are not ordered")
      } else {
        if(e.index != prevIndex+1) unexpected("Sequence entries are not strictly ordered")
      }
      prevIndex = e.index
      if(lookup(e.index).contains(e)) unexpected("Entry inconsistent with lookup result")
      check(e)
    }
  }


  def validateModule(module: Module):Unit = {
    //Check that their are no duplicates and that it is ordered and validate it
    processOrdered[FunctionDef](module.functions,module.function, f => validateFunction(f,module), exact = false)
    processOrdered[DataType](module.dataTypes,module.dataType, d => validateDataType(d,module), exact = false)
    processOrdered[Risk](module.risks,module.risk, _ => (), exact = false)
  }

  private def getModuleForced(h:Hash, current:Module):Module ={
    if(h == current.hash) return current
    ModuleRegistry.moduleByHash(h) match {
      case Some(value) => value
      case None => unexpected("Requested Module not present")
    }
  }

  private def checkOnModule(h:Hash, current:Module, check: Module => Unit):Unit = check(getModuleForced(h,current))

  private def checkTypeCaps(t:Type, reqCaps:Set[Capability], container:TypeParameterized,  current:Module, promoteGenerics:Boolean = false):Unit = {
    t match {
      case RealType(module, offset, applies) =>
        val m = getModuleForced(module,current)
        val dt = m.dataType(offset).getOrElse(unexpected("Requested Datatype does not exist"))
        for(c <- reqCaps) {
          if(!dt.capabilities.contains(c)) unexpected("Required Capability not present")
        }
        val recCaps = reqCaps.filter(c => c.recursive)
        if(recCaps.nonEmpty && applies.nonEmpty){
          applies.zip(dt.generics).filter(ag => !ag._2.phantom).map(ag => ag._1).foreach(t => checkTypeCaps(t, recCaps, container, current, promoteGenerics))
        }
      case GenericType(capabilities, pos) =>
        //todo: have an assert function on error handler
        assert(capabilities == container.generics(pos).capabilities)
        val provCaps = if(promoteGenerics) {
          capabilities ++ Capability.recursives
        } else {
          capabilities
        }
        for(c <- reqCaps) {
          if(!provCaps.contains(c)) unexpected("Required Capability not present")
        }
      case _ =>
    }
  }

  private def checkTypes(t:Type, container:TypeParameterized, current:Module):Unit ={

    def checkWithModule(m:Module,offset:Int, applies:Seq[Type]): Unit = {
      val dt = m.dataType(offset).getOrElse(unexpected("Requested Datatype does not exist"))
      if (dt.generics.length == applies.length) unexpected("Applied generics missmatch specified generics on data type")
      dt.generics.zip(applies).foreach(ag => {
        //check recursively
        checkTypes(ag._2,container,current)
        //check constraints
        checkTypeCaps(ag._2,ag._1.capabilities,container,current)
        //check that we never use a phantom as real
        ag._2 match {
          case GenericType(_,pos) =>
            val gd = container.generic(pos).getOrElse(unexpected("Requested Generic does not exist"))
            if(gd.phantom && !ag._1.phantom) {
              unexpected("Phantom type missmatch of generic application")
            }
          case _ =>
        }
      })
    }

    t match {
      case LocalType(offset, applies) => checkWithModule(current,offset,applies)
      case RealType(module, offset, applies) => checkOnModule(module,current, m => checkWithModule(m,offset,applies))
      case NativeType(typ, applies) =>
        //todo
        // 1: number of args correct
        // 2: recursively check args
        // 3: type of args correct (inkl constraints)
        // 4: check that phantom constraints are correct
        ???
      case ImageType(typ) => checkTypes(typ, container, current)
      case GenericType(_,offset)=> if(container.generic(offset).isEmpty){
        unexpected("Generic with requested index does not exist")
      }
    }
  }

  private def validateDataType(data:DataType, current:Module):Unit = {

    processOrdered[Generic](data.generics,data.generic, g => {
      if(g.protection) unexpected("protection on data type generic not supported")
    })
    val reqCaps =  data.capabilities.filter(c => c.recursive) + Capability.Embed
    processOrdered[Constructor](data.constructors,data.constructor, c => {
      processOrdered[Field](c.fields,c.field, field => {
        checkTypes(field.typ, data, current)
        checkTypeCaps(field.typ, reqCaps, data, current, promoteGenerics = true)
        field.typ match {
          case GenericType(_,pos) =>
            val gd = data.generic(pos).getOrElse(unexpected("Requested Generic does not exist"))
            if(gd.phantom) unexpected("Phantoms can not be used as field types")
          case _ =>
        }
      })
    })
  }

  private def validateFunction(function:FunctionDef, current:Module):Unit = {
    for(r <- function.risks) {
      r match {
        case types.Risk.Local(offset) =>
          if(!current.risk(offset).contains(r)){
            unexpected("Specified risk not equal to lookuped risk")
          }
        case types.Risk.Module(module, offset) =>
          checkOnModule(module, current, m => {
            if(!m.risk(offset).contains(r)){
              unexpected("Specified risk not equal to lookuped risk")
            }
          })
        case types.Risk.Native(_) =>
      }
    }


    processOrdered[Generic](function.generics,function.generic, g => {
      if(g.protection && function.visibility != Visibility.Protected){
        unexpected("Protected generics on unprotected functions are not allowed")
      }
    })

    processOrdered[Param](function.params,function.param, p => {
      checkTypes(p.typ, function, current)
      p.typ match {
        case GenericType(_, pos) =>
          val gf = function.generic(pos).getOrElse(unexpected("Requested Generic does not exist"))
          if(gf.phantom){
            unexpected("Phantom generics not allowed as parameter type")
          }
        case _ =>
      }
    })

    var idents:Set[String] = function.params.map(p => p.name).toSet

    processOrdered[Result](function.results,function.result, r => {
      checkTypes(r.typ, function, current)
      if(!r.borrows.forall(b => idents.contains(b))){
        unexpected("Illegal Borrow target")
      }
      //the remaining borrows can refer to this return value
      idents = idents + r.name
      r.typ match {
        case GenericType(_, pos) => {
          val gf = function.generic(pos).getOrElse(unexpected("Requested Generic does not exist"))
          if(gf.phantom){
            unexpected("Phantom generics not allowed as result type")
          }
        }
        case _ =>
      }
    })

    function.code match {
      case None =>
      case Some(c) => CodeValidator.validateCode(c,function,current)
    }

  }

}
