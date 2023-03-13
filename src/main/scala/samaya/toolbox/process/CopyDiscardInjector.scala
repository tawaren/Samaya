package samaya.toolbox.process

import samaya.structure.{Attribute, Binding, FunctionDef, Generic, ImplementDef, Package, Param, Result, Transaction, types}
import samaya.structure.types.{SourceId, _}
import samaya.toolbox.stack.SlotFrameStack.SlotDomain
import samaya.toolbox.track.TypeTracker
import samaya.toolbox.transform.{EntryTransformer, TransformTraverser}
import samaya.toolbox.traverse.ViewTraverser
import samaya.types.Context

import scala.collection.immutable.ListMap

object CopyDiscardInjector extends EntryTransformer {

  override def transformFunction(in: FunctionDef, context: Context): FunctionDef = {
    val analyzer = new LastUsageFinder(Left(in),context)
    val result = analyzer.extract()
    val transformer = new CopyDropInjection(result,Left(in),context)
    new FunctionDef {
      override val src:SourceId = in.src
      override val code: Seq[OpCode] = transformer.transform()
      override val external:Boolean = in.external
      override val index: Int = in.index
      override val name: String = in.name
      override val attributes: Seq[Attribute] = in.attributes
      override val accessibility: Map[Permission, Accessibility] = in.accessibility
      override val generics: Seq[Generic] = in.generics
      override val params: Seq[Param] = in.params
      override val results: Seq[Result] = in.results
      override val transactional: Boolean = in.transactional
      override val position: Int = in.position
    }
  }


  override def transformImplement(in: ImplementDef, context: Context): ImplementDef = {
    val analyzer = new LastUsageFinder(Right(in),context)
    val result = analyzer.extract()
    val transformer = new CopyDropInjection(result,Right(in),context)
    new ImplementDef {
      override val src:SourceId = in.src
      override val code: Seq[OpCode] = transformer.transform()
      override val external:Boolean = in.external
      override val index: Int = in.index
      override val name: String = in.name
      override val attributes: Seq[Attribute] = in.attributes
      override val accessibility: Map[Permission, Accessibility] = in.accessibility
      override val generics: Seq[Generic] = in.generics
      override val params: Seq[Param] = in.params
      override val results: Seq[Result] = in.results
      override val position: Int = in.position
      override val sigParamBindings: Seq[Binding] = in.sigParamBindings
      override val sigResultBindings: Seq[Binding] = in.sigResultBindings
      override val transactional: Boolean = in.transactional
    }
  }

  case class AnalysisResults(
      injectionsByCodeId:Map[SourceId,Seq[Val]],
      lastUsagesOfValue:Map[Val,Set[SourceId]]
  )

  //todo: update to final algo

  // Strategy (Analysis):
  //      Ignore(Except Discards): Copy + Drop Types
  //      Ignore(Except Discards): Non-Consume input params
  //      Ignore: NonConsumings
  //        The Ignore eliminates false positive where we could have eliminated the usage through coping
  //      For the remaining opcodes/type combos track the last usage
  //       If it is not used in a branch but used in another then we add a virtual usage to the branches as well
  //       This allows to insert Discards to guarantee a use in all branches or none
  //       The whole branching opcode counts as one usage and thus a single usage after the branches join is enough to become the last usage instead off the one in the branches

  // Strategy (Injection):
  //      On a Fetch/Switch/Unpack/Pack
  //        Evaluate for each if we should copy
  //         Copy if it has Copy & Not-Last use (not in Last use map = Not Last use)
  //        If all are copied use the CopyFetch Mode
  //        If 1+ is not copied use the MoveFetchMode and implicitly copy the copies
  //      On a Invoke/TryInvoke/InvokeSig/InvokeTrySig/Return/Rollback call
  //        Evaluate for each param if we should copy
  //         Copy if it has Copy & Consuming Param & Not-Last use (not in Last use map = Not Last use)
  //        For each copy add a implicit copy opcode
  //      Ignore Rest



  //this tracks all Vals that either have Copy + Drop | Or are Non-Consume Params)
  // Reason: we can always copy them and thus do not need to record usages (except for explicit consumes (which includes discards))
  //    Copy + Drop: we just copy on every use and even if not used after that it can be Dropped
  //    Non-Consumes: Non-Consumes can never be moved and they are returned implicitly
  object AlwaysCopy extends SlotDomain[Boolean] {
    //On a merge we choose the one with the most tracking
    override def merge(vals: Seq[Boolean]): Option[Boolean] = vals.reduceOption(_ & _)
  }

  object Owned extends SlotDomain[Boolean] {
    //On a merge we choose the owned
    override def merge(vals: Seq[Boolean]): Option[Boolean] = vals.reduceOption(_ | _)
  }

  //todo: change source resolution everywhere to before super
  //       but only after it works maybe their was a reason we did it after even if it seams wrong
  class LastUsageFinder(override val entry:Either[FunctionDef,ImplementDef], override val context:Context) extends ViewTraverser with TypeTracker{

    //todo: propagate the usedOver -- What to Set for injected
    sealed trait IdUsage {def usedBy:SourceId}
    case class Injected(override val usedBy:SourceId) extends IdUsage {}
    case class Real(override val usedBy:SourceId) extends IdUsage

    sealed trait Usage {
      def allIds:Set[IdUsage]
    }

    case class SingleUsage(id:IdUsage) extends Usage {
      def allIds:Set[IdUsage] = Set(id)
    }

    case class BranchUsage(override val allIds:Set[IdUsage]) extends Usage


    class Branch {
      //The Usages for the block currently under processing
      var potentialConsumes:Map[Val,Usage] = Map.empty
      //var touches:Map[SourceId, Set[Val]] = Map.empty

      def recordPotentialConsume(v:Val, u:Usage):Unit = {
        potentialConsumes = potentialConsumes.updated(v,u)
        /*touches = u.allIds.map(_.usedBy).foldLeft(touches) {
          case (t,s) => t.updated(s,v)
        }*/
      }

    }

    //frame holding all the relevant information needed to track last usages
    class Frame(
       //number of branches in this frame that are not yet processed
       var branches:Int,
       //The usage metrics for each branch in the frame (for the already processed ones)
       var blocks:Seq[(SourceId,Branch)] = Seq.empty,
       //The Usages for the block currently under processing
       var current:Branch = new Branch()
     )

    //Todo: Can the new StateDomainReplace This?
    //we do a parallel frame tracking as Normal Tracker is not Powerful enough
    private var stack = Seq[Frame]()

    //Some Helpers that give reasonable names top operations
    // Mark a Val as used, only the last will survive as we overwrite previous ones
    def recordUsage(v:Val, u:Usage): Unit = {
      stack.head.current.recordPotentialConsume(v,u)
    }
    def recordUsage(v:Val, c:SourceId): Unit = {
      recordUsage(v,SingleUsage(Real(c)))
    }

    //As recordUsage but filters non consuming operations out
    def checkedRecord(v:Val, c:SourceId, stack:Stack, canConsume:Boolean = true): Unit =  if(canConsume && !stack.readSlot(AlwaysCopy,v).getOrElse(false)) recordUsage(v,c)

    //Populates the discardOnly Set based on some properties (Copy / Drop)
    def introduceVal(stack:Stack, v:Val, readOnly:Boolean = false): Stack = {
        val nStack = stack.updateSlot(Owned,v)(_ => Some(true))
        val typ = stack.getType(v)
        if(readOnly || (typ.hasCap(context,Capability.Copy) && typ.hasCap(context, Capability.Drop))) {
          nStack.updateSlot(AlwaysCopy,v)(_ => Some(true))
        } else {
          nStack.updateSlot(AlwaysCopy,v)(_ => Some(false))
        }
    }


    //Helper to Start new frames for Switch & Try
    def newScope(branches:Int): Unit = {
      assert(branches != 0)
      stack = new Frame(branches) +: stack
    }

    //Closes a Block in a Frame
    // This does the heavy liftitng of propagating last usages from one block to the other blocks if the other does not use it as well
    // This guarantees that either all branches have a last usage or none
    // It further merges the usages of all Branches into a single BranchUsage
    def endBlock(bid: SourceId): Unit = {
      //get the active frame
      val head = stack.head
      //record the current
      head.blocks = (bid, head.current) +: head.blocks
      //tell the frame that one block less is expected
      head.branches -= 1
      //Check if this was the last Block in the Frame
      if(head.branches != 0) {
        //more to come reset the current
        head.current = new Branch()
      } else {
        //Was the last one, do the heavy lifting of merging them into the parent frames current
        //drop the active frame
        stack = stack.tail
        //get all the branches
        val branches = head.blocks
        //find all Vals that are used in at least one branch
        val allUsages = branches.flatMap(_._2.potentialConsumes).map(_._1).toSet
        //go over all used Vals
        for(v <- allUsages) {
          //for each Val collect the Vals from all branches (and inject virtual last uses)
          // note: this injects to much even local ones but the return transformer will filter them out
          val newUsages = BranchUsage(branches.flatMap{
            case (bid, branch) => branch.potentialConsumes.get(v) match {
              //For branches where the value was not used, enforce a use to ensure it is used on all branches
              case None => Set[IdUsage](Injected(bid)) //bid is the return/block opcode of the branch
              //If it was used just take the existing uses
              case Some(u) => u.allIds
            }
          }.toSet)
          //record it in the parent frame
          //if it is used later in the parent frame this will be overwritten
          recordUsage(v, newUsages)
        }
      }
    }

    //this is the main method that starts the whole process
    def extract(): AnalysisResults = {
      //introduce a caller frame
      newScope(1)
      //do the traversing (we are not interested in the result as we track separately)
      super.traverse()
      //ensure that only the caller remains on the stack
      assert(stack.size == 1)
      //init new empty stuff to collect aggregated results
      var injectionsByCodeId = Map.empty[SourceId,Seq[Val]].withDefaultValue(Seq.empty)
      var lastUsagesOfValue = Map.empty[Val,Set[SourceId]].withDefaultValue(Set.empty)
      //go over all last usages of the block
      for((v,u) <- stack.head.current.potentialConsumes){

        //copy to LastUseBy
        lastUsagesOfValue = lastUsagesOfValue.updated(v,u.allIds.flatMap{
          case Injected(_) => None
          case Real(usedBy) => Some(usedBy)
        })

        //populate the reverse lookup map containing the injections
        for(c <- u.allIds) {
          injectionsByCodeId = c match {
            case Injected(usedAt) => injectionsByCodeId.updated(usedAt,injectionsByCodeId.getOrElse(usedAt,Seq.empty) :+ v)
            case _ => injectionsByCodeId
          }
        }
      }
      //Pack and return it
      AnalysisResults(injectionsByCodeId, lastUsagesOfValue)
    }

    override def functionStart(params: Seq[Param], origin: SourceId, stack: Stack): Stack = {
      val nStack = super.functionStart(params, origin, stack)
      //collect the params
      val consumeParamVals = params.filter(_.consumes).map(getParamVal)
      val untouchedParamVals = params.filterNot(_.consumes).map(getParamVal)
      //Threat a function like a normal branch
      newScope(1)
      //note: Order is irrelevant as we just set val bindings and do not read them
      //Non-Consuming are readOnly and thus get the alwaysCopy
      val nStack2 = untouchedParamVals.foldLeft(nStack)(introduceVal(_,_, readOnly = true))
      //Consuming are treated like moves / owned
      consumeParamVals.foldLeft(nStack2)(introduceVal(_,_))
      //Note: the branch will be closed by the block ending
    }

    override def traverseBlockEnd(assigns: Seq[Id], origin: SourceId, stack: Stack): Stack = {
      endBlock(origin)
      super.traverseBlockEnd(assigns, origin, stack)
    }

    override def traverseJoin(rets: Seq[Id], origin: SourceId, stacks: Seq[Stack]): Stack = {
      val nStack = super.traverseJoin(rets, origin, stacks)
      rets.map(aid => nStack.resolve(aid)).foldLeft(nStack)(introduceVal(_,_))
    }

    override def lit(res: TypedId, value: Const, origin: SourceId, stack: Stack): Stack = {
      val nStack = super.lit(res, value, origin, stack)
      introduceVal(nStack,nStack.resolve(res.id))
    }

    override def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      checkedRecord(stack.resolve(src),origin,stack, mode != FetchMode.Copy)
      val nStack = super.fetch(res, src, mode, origin, stack)
      introduceVal(nStack,nStack.resolve(res))
    }

    override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      checkedRecord(stack.resolve(src),origin,stack,mode != FetchMode.Copy)
      val nStack = super.unpack(res, src, mode, origin, stack)
      res.map(nStack.resolve).foldLeft(nStack)(introduceVal(_,_))
    }

    override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      checkedRecord(stack.resolve(src),origin,stack,mode != FetchMode.Copy)
      val nStack = super.field(res, src, fieldName, mode, origin, stack)
      introduceVal(nStack,nStack.resolve(res))
    }

    override def pack(res: TypedId, srcs: Seq[Ref], ctr:Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      srcs.map(aid => stack.resolve(aid)).foreach(checkedRecord(_,origin,stack,mode != FetchMode.Copy))
      val nStack = super.pack(res, srcs, ctr, mode, origin, stack)
      introduceVal(nStack,nStack.resolve(res.id))
    }

    override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      func.paramInfo(context).zip(params.map(stack.resolve)).foreach{
        case ((_,consumes), pval) => checkedRecord(pval,origin, stack, consumes)
      }
      val nStack = super.invoke(res, func, params, origin, stack)
      res.map(nStack.resolve).foldLeft(nStack){
        case (s, pval) => introduceVal(s, pval)
      }
      nStack
    }

    override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      checkedRecord(stack.resolve(src),origin,stack)
      val srcType = stack.getType(src)
      srcType match {
        case sig:SigType =>
          sig.paramInfo(context).zip(params.map(stack.resolve)).foreach{
            case ((_,consumes), pval) => checkedRecord(pval,origin, stack, consumes)
          }

          val nStack = super.invokeSig(res, src, params, origin, stack)

          res.map(nStack.resolve).foldLeft(nStack){
            case (s, pval) => introduceVal(s, pval)
          }
        case _ => super.invokeSig(res, src, params, origin, stack)
      }
    }

    override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
      func.paramInfo(context).zip(params.map(p => stack.resolve(p._2))).foreach{
        case ((_,consumes), pval) => checkedRecord(pval,origin, stack, consumes)
      }
      val nStack = super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
      newScope(2)
      nStack
    }

    override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
      val srcType = stack.getType(src)
      srcType match {
        case sig:SigType =>
          sig.paramInfo(context).zip(params.map(p => stack.resolve(p._2))).foreach{
            case ((_,consumes), pval) => checkedRecord(pval,origin, stack, consumes)
          }
        case _ =>
      }
      val nStack = super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
      newScope(2)
      nStack
    }

    override def invokeSuccStart(fields: Seq[AttrId], call: Either[Func, Ref], origin: SourceId, stack: Stack): Stack = {
      val nStack = super.invokeSuccStart(fields, call, origin, stack)
      fields.map(nStack.resolve).foldLeft(nStack)(introduceVal(_,_))
    }

    override def invokeFailStart(fields: Seq[AttrId], call: Either[Func, Ref], essential: Seq[Boolean], origin: SourceId, stack: Stack): Stack = {
      val nStack = super.invokeFailStart(fields, call, essential, origin, stack)
      fields.map(nStack.resolve).foldLeft(nStack)(introduceVal(_,_))
    }

    override def discard(trg: Ref, origin: SourceId, stack: Stack): Stack = {
      //is always a use as we do explicitly drop it no way around it
      recordUsage(stack.resolve(trg),origin)
      super.discard(trg, origin, stack)
    }

    override def letBefore(res: Seq[AttrId], block: Seq[OpCode], origin: SourceId, stack: Stack): Stack = {
      val nStack = super.letBefore(res, block, origin, stack)
      newScope(1)
      nStack
    }

    override def switchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      checkedRecord(stack.resolve(src),origin,stack, mode != FetchMode.Copy)
      val nStack = super.switchBefore(res, src, branches, mode, origin, stack)
      newScope(branches.size)
      nStack
    }

    override def inspectSwitchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, state: Stack): Stack = {
      val nStack = super.inspectSwitchBefore(res, src, branches, origin, state)
      newScope(branches.size)
      nStack
    }

    override def caseStart(fields: Seq[AttrId], src: Ref, ctr: Id, mode: Option[FetchMode], origin: SourceId, stack: Stack): Stack = {
      val nStack = super.caseStart(fields, src, ctr, mode, origin, stack)
      fields.map(nStack.resolve).foldLeft(nStack)(introduceVal(_,_, mode.isEmpty))
    }

    override def project(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
      checkedRecord(stack.resolve(src),origin,stack, canConsume = false)
      val nStack = super.project(res, src, origin, stack)
      introduceVal(nStack,nStack.resolve(res))
    }

    override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
      checkedRecord(stack.resolve(src),origin,stack, canConsume = false)
      val nStack =  super.unproject(res, src, origin, stack)
      introduceVal(nStack,nStack.resolve(res))
    }

    override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      params.map(aid => stack.resolve(aid)).foreach(checkedRecord(_,origin, stack))
      val nStack = super.rollback(res, resTypes, params, origin, stack)
      res.map(nStack.resolve).foldLeft(nStack)(introduceVal(_,_))
    }

    override def _return(res: Seq[AttrId], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      params.map(aid => stack.resolve(aid)).foreach(checkedRecord(_,origin, stack))
      val nStack = super._return(res, params, origin, stack)
      res.map(nStack.resolve).foldLeft(nStack)(introduceVal(_,_))
    }
  }

  class CopyDropInjection(lookup:AnalysisResults, override val entry:Either[FunctionDef,ImplementDef], override val context:Context) extends TransformTraverser with TypeTracker {

    //sometimes we create n opcode that does not require further processing by this transformer
    private var ignoreOpcodes:Set[SourceId] = Set.empty

    private def copyDuplicateVals(inVals:Seq[Val], source:SourceId):(Seq[OpCode], Seq[Ref]) = {
      var exists = Set.empty[Ref]
      val res = for(r <- inVals) yield {
        if(exists.contains(r)) {
          val newId = Id.apply(r.id)
          //ensure we have a unique fresh one
          val newOrigin = new InputSourceId(source.origin)
          //make a fix ref
          val opCode = OpCode.Fetch(AttrId(newId,Seq.empty), r, FetchMode.Copy, newOrigin)
          (Some(opCode), opCode.retVal(0))
        } else {
          exists += r
          (None,r)
        }
      }
      (res.flatMap(_._1), res.map(_._2))
    }

    override def transformBlock(input: Seq[AttrId], result: Seq[Id], code: Seq[OpCode], origin: SourceId, state: Stack): Seq[OpCode] = {
      if(ignoreOpcodes.contains(origin)) return code
      //Note: the filter(state.exists) removes the values that were entered to eagerly by analyzer
      //      it is way easier to do this here then in Analyzer (but in Analyzer would be conceptually more consistent)
      val injectedVals = lookup.injectionsByCodeId(origin).filter(state.exists).filter(v => state.getType(v).hasCap(context, Capability.Drop))
      if(injectedVals.isEmpty) {
        code
      } else {
        val lastOrigin = code.lastOption.map(_.id).getOrElse(origin)
        val injectOrigin = lastOrigin.deriveSourceId(0)
        val returnOrigin = lastOrigin.deriveSourceId(1)

        ignoreOpcodes = ignoreOpcodes + injectOrigin + returnOrigin

        val discardInjectCode = OpCode.DiscardMany(injectedVals, injectOrigin)
        val lastReal = code.filter(!_.isVirtual).last
        val newRetOpcode = OpCode.Return(lastReal.rets,lastReal.rets.map(_.id), returnOrigin)

        //todo: add before last (and skip ret) if not refered by last opcode  <- Hard to achieve (because of the state we have / not have)
        //      we need to check if ins refer to same (only works with Vals)
        //       if refs we would need the stack at that pos to resolve it but we don't have that yet
        //    In theory we could track touches on block level (even neseted)
        //       if on checkedUsage canConsume == false we just update touches else both
        //      then we can discard at start if no touchy in block
        code :+ discardInjectCode :+ newRetOpcode
      }
    }


    override def transformFetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      if(ignoreOpcodes.contains(origin)) return None
      //ignore non inferable access
      if(mode != FetchMode.Infer) return None
      if(lookup.lastUsagesOfValue(stack.resolve(src)).contains(origin)) {
        //we are the last use, do a Move
        Some(Seq(OpCode.Fetch(res,src,FetchMode.Move,origin)))
      } else {
        //we are not the last use, do a Copy (Note if it is not copiable this leads to an error later on)
        //  but if we wood do a move instead it would lead to an error on the last move opcode later on
        Some(Seq(OpCode.Fetch(res,src,FetchMode.Copy,origin)))
      }
    }

    //Note: we do nothing on a discard, even if we could discard a copy
    //  reason:
    //    if the discard was explicit specified in the source the developer expects an error if the discard fails
    //    if the discard was introduced by another transformation then that transformation made a mistake which we should not silently correct
    //override def transformDiscard(trg: Id, origin: CodeId, stack: Stack): Option[Seq[OpCode]] = super.transformDiscard(trg, origin, stack)
    //override def transformDiscardMany(trg: Seq[Id], origin: CodeId, stack: Stack): Option[Seq[OpCode]] = super.transformDiscardMany(trg, origin, stack)

    override def transformUnpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      if(ignoreOpcodes.contains(origin)) return None
      //ignore non inferable access
      if(mode != FetchMode.Infer) return None

      if(lookup.lastUsagesOfValue(stack.resolve(src)).contains(origin)) {
        //we are the last use, do a Move
        Some(Seq(OpCode.Unpack(res,src,FetchMode.Move,origin)))
      } else {
        //we are not the last use, do a Copy (Note if it is not copiable this leads to an error later on)
        //  but if we wod do a move instead it would lead to an error on the last move opcode later on
        Some(Seq(OpCode.Unpack(res,src,FetchMode.Copy,origin)))
      }
    }

    override def transformField(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Option[Seq[OpCode]] =  {
      if(ignoreOpcodes.contains(origin)) return None
      //ignore non inferable access
      if(mode != FetchMode.Infer) return None
      if(lookup.lastUsagesOfValue(stack.resolve(src)).contains(origin)) {
        //we are the last use, do a Move
        Some(Seq(OpCode.Field(res,src,fieldName,FetchMode.Move,origin)))
      } else {
        //we are not the last use, do a Copy (Note if it is not copiable this leads to an error later on)
        //  but if we wood do a move instead it would lead to an error on the last move opcode later on
        Some(Seq(OpCode.Field(res,src,fieldName,FetchMode.Copy,origin)))
      }
    }

    override def transformSwitch(res: Seq[AttrId], src: Ref, bodies: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Option[Seq[OpCode]] =  {
      if(ignoreOpcodes.contains(origin)) return None
      //ignore non inferable access
      if(mode != FetchMode.Infer) return None
      if(lookup.lastUsagesOfValue(stack.resolve(src)).contains(origin)) {
        //we are the last use, do a Move
        Some(Seq(OpCode.Switch(res,src,bodies,FetchMode.Move,origin)))
      } else {
        //we are not the last use, do a Copy (Note if it is not copiable this leads to an error later on)
        //  but if we wod do a move instead it would lead to an error on the last move opcode later on
        Some(Seq(OpCode.Switch(res,src,bodies,FetchMode.Copy,origin)))
      }
    }

    //todo: Handle the spezial case where src used more then once & is last use, as then we need copy for all expect last
    private def transformParams(params: Seq[(Ref, Boolean)], origin: SourceId, stack: Stack): (Seq[OpCode], Seq[Ref]) = {
      val regionMap = params.map(p => (p._1,p._1.src)).toMap
      val valsWithConsumeInfo = params.map{
        case (id,c) => (stack.resolve(id),c)
      }

      //we need to move at least one
      val newRefsAndFetches = valsWithConsumeInfo.map{ case (v,consume) =>
        if(!consume || lookup.lastUsagesOfValue(v).contains(origin)) {
          //move val
          (v, None)
        } else {
          //copy val
          //keep the name intact just shadow old value
          val nId = AttrId(Id.apply(v.id), Seq.empty)
          //adapt it from the lookup id or use the pack code if missing
          val newOrigin = regionMap.getOrElse(v.id, origin)
          //make a fix ref
          val opCode = OpCode.Fetch(nId,v.id, FetchMode.Copy, newOrigin)
          (opCode.retVal(0), Some(opCode))
        }
      }

      val codes = newRefsAndFetches.flatMap(_._2)
      val newSrcs = newRefsAndFetches.map(_._1)
      //todo: if we have consume & non-consumes prefer to copy the consumes
      val (dedups, finalSrcs) = copyDuplicateVals(newSrcs, origin)
      (codes ++ dedups,finalSrcs)
    }

    override def transformPack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      if(ignoreOpcodes.contains(origin)) return None
      //ignore non inferable access
      if(mode != FetchMode.Infer) return None
      val vals = stack.resolveAll(srcs)
      if(vals.isEmpty) return Some(Seq(OpCode.Pack(res,srcs,ctr,FetchMode.Move,origin)))
      //check if we can do a full copy
      val move = vals.exists(lookup.lastUsagesOfValue(_).contains(origin))
      Some(if(move) {
        //we need to move at least one
        val (extraCodes, finalSrcs) = transformParams(srcs.map((_, true)), origin, stack)
        //we are the last use, do a Move
        extraCodes :+ OpCode.Pack(res,finalSrcs,ctr,FetchMode.Move,origin)
      } else {
        //we can copy everything
        //we are not the last use of any input, do a Copy (Note if it is not copiable this leads to an error later on)
        //  but if we wod do a move instead it would lead to an error on the last move opcode later on
        Seq(OpCode.Pack(res,srcs,ctr,FetchMode.Copy,origin))
      })
    }

    override def transformInvoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      if(ignoreOpcodes.contains(origin)) return None
      val isConsume =  func.paramInfo(context).map(_._2).padTo(params.size, false)
      val (extraCodes, finalSrcs) = transformParams(params.zip(isConsume), origin, stack)
      Some(extraCodes :+ OpCode.Invoke(res,func,finalSrcs,origin))
    }

    override def transformTryInvoke(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], success: (Seq[AttrId], Seq[OpCode]), failure: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      if(ignoreOpcodes.contains(origin)) return None
      val isConsume =  func.paramInfo(context).map(_._2).padTo(params.size, false)
      val (extraCodes, finalSrcs) = transformParams(params.map(_._2).zip(isConsume), origin, stack)
      Some(extraCodes :+ OpCode.TryInvoke(res,func,params.map(_._1).zip(finalSrcs), success, failure, origin))
    }

    override def transformInvokeSig(res: Seq[AttrId], func: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      if(ignoreOpcodes.contains(origin)) return None
      stack.getType(func) match {
        case sdt:SigType =>
          val isConsume =  sdt.paramInfo(context).map(_._2).padTo(params.size, false)
          val (extraCodes, finalSrcs) = transformParams(params.zip(isConsume), origin, stack)
          Some(extraCodes :+ OpCode.InvokeSig(res,func,finalSrcs,origin))
        case _ => None
      }
    }

    override def transformTryInvokeSig(res: Seq[AttrId], func: Ref, params: Seq[(Boolean, Ref)], origin: SourceId, stack: Stack, success: (Seq[AttrId], Seq[OpCode]), failure: (Seq[AttrId], Seq[OpCode])): Option[Seq[OpCode]] = {
      if(ignoreOpcodes.contains(origin)) return None
      stack.getType(func) match {
        case sdt:SigType =>
          val isConsume =  sdt.paramInfo(context).map(_._2).padTo(params.size, false)
          val (extraCodes, finalSrcs) = transformParams( params.map(_._2).zip(isConsume), origin, stack)
          Some(extraCodes :+ OpCode.TryInvokeSig(res,func,params.map(_._1).zip(finalSrcs), success, failure, origin))
        case _ => None
      }
    }

    override def transformReturn(rets: Seq[AttrId], params: Seq[Ref], origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      if(ignoreOpcodes.contains(origin)) return None
      val (extraCodes, finalSrcs) = transformParams(params.map((_,true)), origin, stack)
      //System.out.println(s"EXTRA: ${origin.src.start} -> $extraCodes")
      //System.out.println(s"CONS: ${origin.src.start} -> ${params.map(v => lookup.lastUsagesOfValue(stack.resolve(v).get).map(_.src.start))}")
      Some(extraCodes :+ OpCode.Return(rets,finalSrcs,origin))
    }

    override def transformRollback(rets: Seq[AttrId], params: Seq[Ref], types: Seq[Type], origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      if(ignoreOpcodes.contains(origin)) return None
      val (extraCodes, finalSrcs) = transformParams(params.map((_,true)), origin, stack)
      Some(extraCodes :+ OpCode.RollBack(rets,finalSrcs,types,origin))
    }
  }
}
