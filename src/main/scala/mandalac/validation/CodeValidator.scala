package mandalac.validation

import mandalac.structure.types.{Code, Const, FetchMode, Func, Id, OpCode, Risk, Type, TypedId, Val}
import mandalac.structure.types.Code.{Return, Throw}
import mandalac.structure.{FunctionDef, Module}
import mandalac.compilation.ErrorHandler._
import mandalac.registries.ModuleRegistry
import mandalac.structure.types.Capability.{Consume, Copy, Create, Drop, Inspect}
import mandalac.structure.types.Type.NativeTypeKind
import mandalac.validation.ValidationStack.{Borrowing, Freed, Owned}

//TODO: IN A SECOND VERSION MAKE A FULL INFERENZER THAT HAS HOOKS <-- Can this work if so we can use it in serializer & dependency collector as well
//Note: this is designed for a working compiler
//       meaning it does not try to give good error messages if the compiler produces incorrect code
//       in case that still happens it just aborts
private class CodeValidator(function:FunctionDef, current:Module) {

  //todo: Who ever calls this should check the imports as well
  //todo: Note this needs to be done on dataType checks as well
  //todo: Register current Module temporary in Registry (use dynamic variable)
  def validateFunction(code:Code): Boolean = canProduceErrors {
    ModuleRegistry.executeInModule(current){
      val stackWithParams = function.params.foldLeft(ValidationStack())((s,p) => s.provide(TypedId(Id(p.name), p.typ))._2)
      val bodyStack = ValidationStack(parent = stackWithParams)
      val expect = function.results.map(r => Id(r.name))
      validateCode(code,expect,bodyStack,function.risks) match {
        //the body throws
        case None =>
        case Some(resStack) =>  //the body has a result
          val frame = resStack.frame
          val results = frame.take(function.results.size)
          val params = frame.drop(function.results.size)

          //check results
          for((v,r) <- results.zip(function.results.reverse)){
            //check type
            if(resStack.getType(v) != r.typ) unexpected("Function return type mismatch")
            //check borrows
            val borrows = r.borrows.map(b => resStack.resolve(Id(b)))
            resStack.getStatus(v) match {
              case Freed => unexpected("Freed values can not be returned from a function")
              case Owned(_) => if(borrows.nonEmpty) unexpected("Borrow function return mismatch")
              case Borrowing(borrows2, _) => if(borrows != borrows2) unexpected("Borrow function return mismatch")
            }
          }

          //check params
          for((v,p) <- params.zip(function.params.reverse)){
            //check type
            if(resStack.getType(v) != p.typ) unexpected("Function return type mismatch")
            //check consume
            resStack.getStatus(v) match {
              case Freed => if(!p.consumes) unexpected("function param status mismatch")
              case Owned(_) => if(p.consumes) unexpected("function param status mismatch")
              case Borrowing(_, _) => unexpected("function param status mismatch")
            }
          }
      }
    }
  }

  //todo: add entry instead of type so that we can check the borrows (needs more then entry, needs vals
  def validateCode(code:Code, expected:Seq[Id], stack: ValidationStack, risks:Set[Risk]): Option[ValidationStack] = {
    code match {
      case Return(ops, returns) =>
        val populatedStack = ops.foldLeft(stack)((s,o) => validateOpCode(o,s,risks))
        val cleanedStack = populatedStack.frame.foldLeft(populatedStack)((s,v) =>
          if(!returns.contains(v)){
            s.getStatus(v) match {
              case Freed => s
              case Owned(_) =>
                val t = s.getType(v)
                if(!t.hasCap(current,Drop)) unexpected("Owned values that are not returned must have the Drop capability")
                s.consume(v)
              case Borrowing(_, _) => s.free(v)
            }
          } else {
            s
          }
        )

        if(returns.length != expected.length) {
          unexpected("Return opcode does not return the expected amount of values")
        }

        Some(cleanedStack.returnFrame(returns.zip(expected)))
      case Throw(err) =>
        if(!risks.contains(err)) {
          unexpected("Thrown error is not declared in current Context")
        }
        //None signals that stack was not touched -- nothing to merge if branching
        None
    }
  }


  private def validateOpCode(code:OpCode, stack: ValidationStack, risks:Set[Risk] ): ValidationStack = {
    code match {
      case OpCode.Lit(res, value) => lit(res,value,stack)
      case OpCode.Let(res, block) => let(res, block, stack, risks)
      case OpCode.Fetch(res, src, mode) => fetch(res,src,mode,stack)
      case OpCode.Discard(trg) => discard(trg, stack)
      case OpCode.DiscardMany(trgs) => trgs.foldLeft(stack){ (s, v) => discard(v,s)}
      case OpCode.DiscardBorrowed(value, borrowedBy) => steal(value,borrowedBy,stack)
      case OpCode.Unpack(res, src, mode) => unpack(res,src,mode,stack)
      case OpCode.Field(res, src, pos, mode) => field(res,src,pos,mode,stack)
      case OpCode.Switch(res, src, branches, mode) => switch(res,src,branches,mode,stack,risks)
      case OpCode.Pack(res, src, tag, mode) => pack(res,src,tag,mode,stack)
      case OpCode.Invoke(res, func, param) => invoke(res,func,param,stack,risks)
      case OpCode.Try(res, tryBlock, branches) => _try(res,tryBlock,branches,stack,risks)
      case OpCode.ModuleIndex(res) => moduleIndex(res,stack)
      case OpCode.Image(res, src) => image(res,src,stack)
      case OpCode.ExtractImage(res, src) => extractImage(res,src,stack)
    }
  }

  private def lit(res:TypedId, value:Const, stack: ValidationStack): ValidationStack = {
    val (_, newStack) = stack.provide(res)
    //todo: check that value can be parsed as res
    newStack
  }

  private def let(res:Seq[Id], block:Code, stack: ValidationStack, risks:Set[Risk]): ValidationStack = {
    if(res.distinct.size != res.size) unexpected("Let expression bindings must be uniquely named")
    validateCode(block, res, stack, risks) match {
      case Some(newStack) => newStack
      case None =>
        if (res.nonEmpty) unexpected("Let expression produced wrong amount of values")
        stack
    }
  }

  def fetch(res:Id, src:Val, mode:FetchMode, stack: ValidationStack): ValidationStack = {
    val t = stack.getActive(src).typ
    val id = TypedId(res,t)
    (mode match {
      case FetchMode.Copy =>
        if(!t.hasCap(current,Copy)) unexpected("Copy a value requires copy capability")
        stack.provide(id)
      case FetchMode.Borrow => stack.borrow(id, Set(src))
      case FetchMode.Move => stack.consume(src).provide(id)
    })._2
  }

  def discard(trg:Val, stack: ValidationStack): ValidationStack = {
    stack.getStatus(trg) match {
      case Freed => unexpected("Can not free already freed value")
      case Borrowing(_, _) => stack.free(trg)
      case Owned(_) =>
        val t = stack.getType(trg)
        if(!t.hasCap(current,Drop)) unexpected("Owned values can only be freed if they have the Drop capability")
        stack.consume(trg)
    }
  }

  def steal(value:Val, borrowedBy:Seq[Val], stack: ValidationStack): ValidationStack = {
    val borrowedBySet = borrowedBy.toSet
    if(borrowedBySet.size != borrowedBy.size) unexpected("borrowed by bindings must be distinct")
    stack.getStatus(value) match {
      case Freed => unexpected("Can not steal from an already freed value")
      case Borrowing(_, borrowedBy2) =>
        if(borrowedBySet != borrowedBy2) unexpected("BorrowedBy declaration in steal missmatches")
        stack.stealDiscard(value)
      case Owned(_) => unexpected("Can not steal from an owned value")
    }
  }

  def unpack(res:Seq[TypedId], src:Val, mode:FetchMode, stack: ValidationStack): ValidationStack = {
    val t = stack.getActive(src).typ
    val ctrs = t.ctrs
    if(ctrs.size != 1) {
      unexpected("Can not unpack types with multiple constructors")
    } else {
      //check that whe have the right amount of fields
      val fields = ctrs.head
      if(fields.size != res.size) unexpected("Can not unpack types with multiple constructors")

      //check the fields have the right type
      fields.zip(res.map(tid => tid.typ)).foreach(ab => {
        if(ab._1 != ab._2)  unexpected("Constructor field type does not match expected unpack type")
      })
    }

    mode match {
      case FetchMode.Copy =>
      if(!t.hasCap(current,Inspect) && !t.isCurrentModule) unexpected("Copy a value requires inspect capability")
        //todo: wrong, copy needed on inner -- but is more pessimistic in sanskrit as well currently
        if(!t.hasCap(current,Copy)) unexpected("Copy a value requires copy capability")
        res.foldLeft(stack){(s,e) => s.provide(e)._2}
      case FetchMode.Borrow =>
        if(!t.hasCap(current,Inspect) && !t.isCurrentModule) unexpected("borrow a value out of another requires inspect capability")
        stack.borrowMany(res.toSet,src,exclusive = false)._2
      case FetchMode.Move =>
        if(!t.hasCap(current,Consume) && !t.isCurrentModule) unexpected("upacking a value requires consume capability")
        res.foldLeft(stack.consume(src)){(s,e) => s.provide(e)._2}
    }
  }

  def field(res:TypedId, src:Val, pos:Int, mode:FetchMode, stack: ValidationStack): ValidationStack = {
    val t = stack.getActive(src).typ
    val ctrs = t.ctrs
    val (field,fields) = if(ctrs.size != 1) {
      unexpected("Can not unpack types with multiple constructors")
    } else {
      //check that whe have the right amount of fields
      val fields = ctrs.head
      (if(fields.size >= pos) {
        unexpected("Can not extract non existent field from value")
      } else {
        if(fields(pos) != res.typ) {
          unexpected("Constructor field type does not match expected field type")
        }
        fields(pos)
      }, fields)
    }

    mode match {
      case FetchMode.Copy =>
        if(!t.hasCap(current,Inspect) && !t.isCurrentModule) unexpected("Copy a value out of a another value requires inspect capability")
        if(!field.hasCap(current,Copy)) unexpected("Copy a value out of a another value requires copy capability")
        stack.provide(res)._2
      case FetchMode.Borrow =>
        if(!t.hasCap(current,Inspect) && !t.isCurrentModule) unexpected("borrow a value out of another requires inspect capability")
        stack.borrow(res,Set(src))._2
      case FetchMode.Move =>
        if(!t.hasCap(current,Consume) && !t.isCurrentModule) unexpected("fetching a field in another value requires consume capability")
        fields.zipWithIndex.foreach(tp =>{
          if(tp._2 != pos) {
            if(!tp._1.hasCap(current,Drop)) unexpected("fetching a field in another value requires drop capability on the remaining values")
          }
        })
        stack.provide(res)._2
    }
  }

  def switch(res:Seq[Id], src:Val, branches:Seq[(Seq[TypedId],Code)], mode:FetchMode, stack: ValidationStack, risks:Set[Risk]): ValidationStack = {
    //todo: route type through and check for equallity -- can we include in branchMerge
    branches.foldLeft[Option[ValidationStack]](None)((cRes, _case ) => {
      val unpackedStack = unpack(_case._1, src,mode,stack)
      validateCode(_case._2, res, unpackedStack, risks) match {
        case None => cRes
        case Some(bRes) =>
          Some(cRes match {
            case None => bRes
            case Some(pRes) => pRes.mergeReturnedBranches(bRes)
          })
      }
    }).getOrElse(stack)
  }

  def pack(res:TypedId, srcs:Seq[Val], tag:Int, mode:FetchMode, stack: ValidationStack): ValidationStack = {
    if(!res.typ.hasCap(current,Create) && !res.typ.isCurrentModule)  unexpected("Creating a value requires create capability")
    val paramTypes = srcs.map(v => stack.getActive(v).typ)
    val ctrs = res.typ.ctrs
    if(ctrs.size >= tag) unexpected("Constructor is missing to pack type")
    val fieldTypes = ctrs(tag)
    if(paramTypes != fieldTypes) unexpected("Constructor  doe not match")
    (mode match {
      case FetchMode.Copy =>
        fieldTypes.foreach(t => if(!t.hasCap(current,Copy)){
          unexpected("Copy a value requires Copy capability")
        })
        stack.provide(res)
      case FetchMode.Borrow =>
        if(fieldTypes.isEmpty) unexpected("Can not borrow pack an empty value")
        val uniqueSrcs = srcs.toSet
        if(uniqueSrcs.size != srcs.size) unexpected("Can not borrow pack an value twice")
        stack.borrow(res, uniqueSrcs)
      case FetchMode.Move =>
        srcs.foldLeft(stack)((s,src) => s.consume(src)).provide(res)
    })._2
  }

  def invoke(res:Seq[TypedId], func:Func, param:Seq[Val], stack: ValidationStack, risks:Set[Risk]): ValidationStack = {
    val paramInfo = func.paramInfo(current)
    if(paramInfo.size != param.size) unexpected("Not enough params supplied for function")
    if(param.distinct.size != param.size) unexpected("Can not use a value twice as parameter")
    val consStack = paramInfo.zip(param).foldLeft(stack)((s,tv) => {
      val pType = s.getActive(tv._2).typ
      if(pType != tv._1._1) unexpected("Function Parameter has the Wrong type")
      if(tv._1._2) {
        s.consume(tv._2)
      } else {
        s
      }
    })

    val retInfo = func.returnInfo(current)
    if(retInfo.size != res.size) unexpected("Function returns wrong number of values")
    retInfo.zip(res).foldLeft(consStack)((s,tv) => {
      if(tv._2.typ != tv._1._1) unexpected("Function Return has the Wrong type")
      val borrows = tv._1._2
      (if(borrows.isEmpty){
        s.provide(tv._2)
      } else {
        s.borrow(tv._2,borrows.map(id => s.resolve(id)), exclusive = false)
      })._2
    })
  }

  def _try(res:Seq[Id], tryBlock:Code, branches:Seq[(Risk,Code)], stack: ValidationStack, risks:Set[Risk]): ValidationStack = {
    val newRisks = risks.union(branches.map(rc => rc._1).toSet)

    val tryRes = validateCode(tryBlock, res, stack, newRisks)
    if(tryRes.isEmpty && res.nonEmpty) unexpected("Try expression produced wrong amount of values")
    branches.foldLeft(tryRes)((cRes, _catch ) => {
      validateCode(_catch._2, res, stack, risks) match {
        case None => cRes
        case Some(bRes) =>
          Some(cRes match {
            case None => bRes
            case Some(pRes) => pRes.mergeReturnedBranches(bRes)
          })
      }
    }).getOrElse(stack)
  }

  def moduleIndex(res: Id, stack: ValidationStack): ValidationStack = {
    stack.provide(TypedId(res,Type.NativeType(NativeTypeKind.PrivateId, Seq.empty)))._2
  }

  def image(res:Id, src:Val, stack: ValidationStack): ValidationStack = {
    //Ensure it is not freed
    val slot = stack.getActive(src)
    stack.provide(TypedId(res,Type.ImageType(slot.typ)))._2
  }

  def extractImage(res:Id, src:Val, stack: ValidationStack): ValidationStack = {
    //Ensure it is not freed
    val newTyp = stack.getActive(src).typ match {
      case Type.ImageType(typ@Type.ImageType(_)) => typ
      case _ => unexpected("Can not extract image from non image types ")
    }
    stack.provide(TypedId(res,newTyp))._2
  }

}


//TODO: better document
object CodeValidator {

  def validateCode(code:Code, function:FunctionDef, current:Module):Boolean = new CodeValidator(function,current).validateFunction(code)

}
