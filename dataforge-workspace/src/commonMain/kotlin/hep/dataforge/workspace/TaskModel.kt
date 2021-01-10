/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.workspace

import hep.dataforge.data.DataTree
import hep.dataforge.data.dynamic
import hep.dataforge.data.update
import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.names.toName
import hep.dataforge.workspace.TaskModel.Companion.MODEL_TARGET_KEY

//FIXME TaskModel should store individual propagation of all data elements, not just nodes

/**
 * A model for task execution
 * @param name the name of the task
 * @param meta the meta for the task (not for the whole configuration)
 * @param dependencies a list of direct dependencies for this task
 */
public data class TaskModel(
    val name: Name,
    val meta: Meta,
    val dependencies: Collection<Dependency>,
) : MetaRepr {
    //TODO provide a way to get task descriptor
    //TODO add pre-run check of task result type?

    override fun toMeta(): Meta = Meta {
        "name" put name.toString()
        "meta" put meta
        "dependsOn" put {
            val dataDependencies = dependencies.filterIsInstance<DataDependency>()
            val taskDependencies = dependencies.filterIsInstance<TaskDependency<*>>()
            setIndexed("data".toName(), dataDependencies.map { it.toMeta() }) //Should list all data here
            setIndexed(
                "task".toName(),
                taskDependencies.map { it.toMeta() }) { _, index -> taskDependencies[index].name.toString() }
            //TODO ensure all dependencies are listed
        }
    }

    public companion object {
        public val MODEL_TARGET_KEY: Name = "@target".asName()
    }
}

/**
 * Build input for the task
 */
public suspend fun TaskModel.buildInput(workspace: Workspace): DataTree<Any> = DataTree.dynamic(workspace.context) {
    dependencies.forEach { dep ->
        update(dep.apply(workspace))
    }
}

public interface TaskDependencyContainer {
    public val defaultMeta: Meta
    public fun add(dependency: Dependency)
}

/**
 * Add dependency for a task defined in a workspace and resolved by
 */
public fun TaskDependencyContainer.dependsOn(
    name: Name,
    placement: DataPlacement = DataPlacement.ALL,
    meta: Meta = defaultMeta,
): WorkspaceTaskDependency = WorkspaceTaskDependency(name, meta, placement).also { add(it) }

public fun TaskDependencyContainer.dependsOn(
    name: String,
    placement: DataPlacement = DataPlacement.ALL,
    meta: Meta = defaultMeta,
): WorkspaceTaskDependency = dependsOn(name.toName(), placement, meta)

public fun <T : Any> TaskDependencyContainer.dependsOn(
    task: Task<T>,
    placement: DataPlacement = DataPlacement.ALL,
    meta: Meta = defaultMeta,
): ExternalTaskDependency<T> = ExternalTaskDependency(task, meta, placement).also { add(it) }


public fun <T : Any> TaskDependencyContainer.dependsOn(
    task: Task<T>,
    placement: DataPlacement = DataPlacement.ALL,
    metaBuilder: MetaBuilder.() -> Unit,
): ExternalTaskDependency<T> = dependsOn(task, placement, Meta(metaBuilder))

/**
 * Add custom data dependency
 */
public fun TaskDependencyContainer.data(action: DataPlacementScheme.() -> Unit): DataDependency =
    DataDependency(DataPlacementScheme(action)).also { add(it) }

/**
 * User-friendly way to add data dependency
 */
public fun TaskDependencyContainer.data(
    pattern: String? = null,
    from: String? = null,
    to: String? = null,
): DataDependency =
    data {
        pattern?.let { this.pattern = it }
        from?.let { this.from = it }
        to?.let { this.to = it }
    }

///**
// * Add all data as root node
// */
//public fun TaskDependencyContainer.allData(to: Name = Name.EMPTY): AllDataDependency = AllDataDependency(to).also { add(it) }

/**
 * A builder for [TaskModel]
 */
public class TaskModelBuilder(public val name: Name, meta: Meta = Meta.EMPTY) : TaskDependencyContainer {
    /**
     * Meta for current task. By default uses the whole input meta
     */
    public var meta: MetaBuilder = meta.builder()
    private val dependencies: HashSet<Dependency> = HashSet<Dependency>()

    override val defaultMeta: Meta get() = meta

    override fun add(dependency: Dependency) {
        dependencies.add(dependency)
    }

    public var target: String by this.meta.string(key = MODEL_TARGET_KEY, default = "")


    public fun build(): TaskModel = TaskModel(name, meta.seal(), dependencies)
}


public val TaskModel.target: String get() = meta[MODEL_TARGET_KEY]?.string ?: ""