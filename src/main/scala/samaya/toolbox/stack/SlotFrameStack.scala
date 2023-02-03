package samaya.toolbox.stack

import samaya.structure.types.{AttrId, Id, Ref, SourceId, Val}
import samaya.toolbox.stack.SlotFrameStack.SlotDomain

import scala.collection.immutable.ListMap

object SlotFrameStack {
  trait SlotDomain[V] {
    //merges two values from different branches
    def merge(vals:Seq[V]):Option[V]
  }

  //todo: this is inefficient but that is a concern for much later
  def joinFrames(rets:Seq[Id], origin:SourceId, branches:Seq[SlotFrameStack]):SlotFrameStack = {
    val slots = branches.map(_.slots)
    val vals = slots.map(_.keySet).foldLeft(Set.empty[Val])(_++_)
    var nSlotsBuild = Map.newBuilder[Val,KeyTypedMap[SlotDomain]]
    val frameBranches = branches.map(_.stack)

    //do the Join on the slot less map
    val (nStack, mappings) = FrameStack.joinFrames(rets,origin,frameBranches)
    //inverse the new -> old vals mapping returned from the slotless
    val oldToNewMap = mappings.flatMap{case (k,v) => v.map((_,k))}

    //for each val that exists in at least one branch merge the entries with for that val from each branch
    vals.foreach{ v =>
      nSlotsBuild += v -> KeyTypedMap.mergeAll(slots.flatMap(_.get(v)), new Merger[SlotDomain] {
        //before we do the merge we call substituteVals on the entry to give it a chance for adapting branch local references to their substitutions
        override def merge[V](k: SlotDomain[V], vs: Seq[V]): Option[V] = k.merge(vs)
      })
    }
    //Now all the vals shared between branches are merged and substituted
    // All the vals explicit to a single branch are substituted as well but not necessarily merged
    val interRes = nSlotsBuild.result()
    //Merge the return values, these have different vals on each branch, but mapping contains which have to be merged to what
    val nSlots =  mappings.foldLeft(interRes){ case (slotsAcc,(newVal, oldVals)) =>
      slotsAcc.updated(newVal,KeyTypedMap.mergeAll(oldVals.map(interRes.lift).flatten,new Merger[SlotDomain] {
        override def merge[V](k: SlotDomain[V], vs: Seq[V]): Option[V] = k.merge(vs)
      }))
    }

    //construct the result
    SlotFrameStack(nSlots,nStack)
  }
}

case class SlotFrameStack(
     //public for debuging //todo
     slots:Map[Val,KeyTypedMap[SlotDomain]] = Map.empty,
     stack:FrameStack = FrameStack()
) {

  def stackSize():Int = stack.stackSize()

  def resolveAll(ids:Seq[Ref]):Seq[Val] = stack.resolveAll(ids)
  def resolveAll(ids:Set[Id]):Set[Val] = stack.resolveAll(ids)
  def resolve(id:Ref):Val = stack.resolve(id).getOrElse(Val.unknown(id.name, id.src)).withAdaptedSource(id.src)
  def resolve(aid:AttrId):Val = stack.resolve(aid.id).getOrElse(Val.unknown(aid.name, aid.src)).withAdaptedSource(aid.src)

  def exists(key:Val):Boolean = stack.exists(key)

  private def readSlots(key:Val):Option[KeyTypedMap[SlotDomain]] = slots.get(key)
  def readExisitng[S](domain:SlotDomain[S], keys:Seq[Ref]):Seq[S] = keys.flatMap(readSlot(domain,_))
  def readSlot[S](domain:SlotDomain[S], key:Ref):Option[S] = readSlot(domain,resolve(key))
  def readSlot[S](domain:SlotDomain[S], key:Val):Option[S] = readSlots(key).flatMap(_.get(domain))

  def updateSlot[S](domain:SlotDomain[S], key:Ref)(sl:Option[S] => Option[S]):SlotFrameStack = updateSlot(domain,resolve(key))(sl)
  def updateSlot[S](domain:SlotDomain[S], key:Val)(sl:Option[S] => Option[S]):SlotFrameStack = {
    slots.get(key) match {
      case None => this
      case Some(res) => sl(res.get(domain)) match {
        case None => copy(slots = slots.updated(key,res.removed(domain)))
        case Some(v) => copy(slots = slots.updated(key,res.updated(domain,v)))
      }
    }
  }

  def frameSize:Int = stack.frameSize
  def frameValues:Seq[Val] = stack.frameValues

  //for debugging:
  def bindings = stack.bindings

  def push(src:Id, origin:SourceId, pos:Int):SlotFrameStack = {
    val (nVal, nStack) =  stack.push(src,origin,pos)
    copy( slots = slots.updated(nVal, KeyTypedMap.empty()),stack = nStack)
  }

  def openFrame():SlotFrameStack = copy(stack = stack.openFrame())
  def closeFrame(numKeep:Int,origin: SourceId):SlotFrameStack = copy(stack = stack.closeFrame(numKeep,origin))
}
