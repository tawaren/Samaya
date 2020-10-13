package samaya.structure
import samaya.structure.types.{Accessibility, Capability, Permission, SourceId}

trait Transaction extends Component {
  def name: String
  def language:String
  def version:String
  def classifier:Set[String]
  def transactional: Boolean
  def params: Seq[Param]
  def results: Seq[Result]
  def attributes: Seq[Attribute]
  def src: SourceId

  override def toInterface(meta: Meta): Interface[Component] = new TransactionInterface(meta, this)

}

object Transaction {
  implicit class TransactionFunctionSig(val txt:Transaction) extends FunctionSig{
    override def index: Int = 0
    override def position: Int = 0
    override def accessibility: Map[Permission, Accessibility] = Map(Permission.Call -> Accessibility.Global)
    override def generics: Seq[Generic] = Seq.empty
    override def attributes: Seq[Attribute] = txt.attributes
    override def capabilities: Set[Capability] = Set.empty

    override def name: String = txt.name
    override def transactional: Boolean = txt.transactional
    override def params: Seq[Param] = txt.params
    override def results: Seq[Result] = txt.results
    override def src: SourceId = txt.src
  }
}
