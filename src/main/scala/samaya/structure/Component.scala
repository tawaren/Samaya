package samaya.structure

import samaya.structure.types.SourceId

trait Component {
    def name: String
    def language: String
    def version: String
    def classifier: Set[String]
    def isVirtual:Boolean
    def src:SourceId

    def toInterface(meta: Meta): Interface[Component]

    def asModule: Option[Module] = this match {
        case module: Module => Some(module)
        case _ => None
    }

    def asModuleInterface:Option[Module with Interface[_]]  = this match {
        case module: Module with Interface[_] => Some(module)
        case _ => None
    }

}


object Component {
    trait ComponentType{
        def classifier:String
        def extension:String
    }

    case object MODULE extends ComponentType {
        override def classifier: String = "module"
        override def extension: String = "mod.sans"
    }

    case object TRANSACTION extends ComponentType {
        override def classifier: String = "transaction"
        override def extension: String = "txt.sans"
    }

}