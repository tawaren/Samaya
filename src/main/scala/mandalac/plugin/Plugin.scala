package mandalac.plugin


//A interfaces for plugins that can check if a certain task can be handled by a specific plugin
trait Plugin {
  type Selector
  //return true if S can be handled by this
  def matches(s:this.type#Selector):Boolean
}
