package mandalac.plugin.impl.wp.json

import mandalac.types.{Location, Package, Path, Workspace}

class WorkspaceImpl(
                     override val name: String,
                     override val workspaceLocation: Location,
                     override val includes: Option[Set[Workspace]],
                     override val dependencies: Option[Set[Package]],
                     override val modules: Option[Set[Path]],
                     override val sourceLocation: Location,
                     override val codeLocation: Location,
                     override val interfaceLocation: Location
) extends Workspace