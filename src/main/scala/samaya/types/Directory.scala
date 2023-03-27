package samaya.types

trait Directory extends GeneralSource with GeneralSink{
  def name:String
  def isRoot:Boolean
  //resolves relative Addresses
  def resolveAddress(address: Address):Address
}
