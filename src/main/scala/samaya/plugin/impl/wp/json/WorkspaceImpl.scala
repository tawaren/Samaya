package samaya.plugin.impl.wp.json

import samaya.structure.LinkablePackage
import samaya.types.{Address, Directory, Repository, Workspace}

class WorkspaceImpl(
                     override val name: String,
                     override val location: Directory,
                     override val includes: Option[Set[Workspace]],
                     override val repositories: Option[Set[Repository]],
                     override val dependencies: Option[Set[LinkablePackage]],
                     override val sources: Option[Set[Address]],
                     override val sourceLocation: Directory,
                     override val codeLocation: Directory,
                     override val interfaceLocation: Directory
) extends Workspace
