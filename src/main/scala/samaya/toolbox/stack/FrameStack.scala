package samaya.toolbox.stack

import samaya.structure.types.{Id, Ref, SourceId, Val}

import scala.collection.immutable.HashMap

//Provides a frame to manage a stack
// It enforces some constraints necessary to o this tracking
// It checks:
//  1: That referred values exist in the scope
//  2: That joined branches result in the same stack layout at the end
//  3: That only related branches are joined / merged

object FrameStack {

  def joinFrames(rets:Seq[Id], origin:SourceId, branches:Seq[FrameStack]):(FrameStack,Map[Val,Seq[Val]]) = {
    assert(branches.nonEmpty)
    val parents = branches.map(_.parent)
    val parent = parents.head
    assert(parents.forall(_.eq(parents.head)))
    val min = branches.minBy(_.frameSize).frameSize
    val branchRes = branches.map(_.frameValues.take(min).reverse).transpose.padTo(rets.size, Seq.empty)
    rets.zip(branchRes).zipWithIndex.foldLeft((parent,Map.empty[Val,Seq[Val]])) {
      case ((p,m),((ret, vs),idx)) =>
        val (nVal, nStack) = p.push(ret,origin,idx)
        val nm = m.updated(nVal,vs)
        (nStack, nm)
    }
  }
}

case class FrameStack(
     //public for debuging //todo
     bindings:HashMap[Id,Val] = HashMap[Id,Val] (),
     stack:Seq[Val] = Seq[Val](),
     parent:FrameStack = null
 ) {

  private lazy val parentSize:Int = if(parent == null) 0 else parent.stackSize()

  def stackSize():Int = parentSize+stack.size

  def resolveAll(ids:Seq[Ref]):Seq[Val] = ids.map(id => resolve(id).getOrElse(Val.unknown(id.name, id.src)))
  def resolveAll(ids:Set[Id]):Set[Val] = ids.map(id => resolve(id).getOrElse(Val.unknown(id.name, id.src)))

  def exists(key:Val):Boolean = {
    if(stack.contains(key)){
      true
    } else if(parent != null) {
      parent.exists(key)
    } else {
      false
    }
  }


  def resolve(id:Ref):Option[Val] = {
    id match {
      //todo: the exist check is inefficent but if this fails we will have an hard time debugging so at least for development do it
      case v:Val if exists(v) => Some(v)
      case _:Val => None
      case id:Id => bindings.get(id) match {
        case None if parent != null => parent.resolve(id)
        case x => x
      }
    }
  }


  def frameSize:Int = stack.size
  def frameValues:Seq[Val] = stack
  def push(src:Id, origin:SourceId, pos:Int):(Val, FrameStack) = {
    //Note: values are identity based so this is guaranteed to be unique and does only equal to itself
    val v = Val(src,origin,pos)
    //if this is not true then the caller made a mistake
    (v, copy(
      stack = v+:stack ,
      bindings = bindings.updated(v.id, v)
    ))
  }

  def openFrame():FrameStack = FrameStack(parent = this)

  //After this all bindings are removed and the lexical scope is closed
  //it can / must now be used in joinFrames
  def closeFrame(numKeep:Int, origin: SourceId):FrameStack = {
    copy(
      bindings = HashMap.empty,
      stack = stack.take(numKeep),
    )
  }
}
