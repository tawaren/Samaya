package samaya.structure

trait Component {
    def name: String
    def language: String
    def version: String
    def classifier: Set[String]
    def isVirtual:Boolean

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
    val MODULE_CLASSIFIER = "module"
    val TRANSACTION_CLASSIFIER = "transaction"
}