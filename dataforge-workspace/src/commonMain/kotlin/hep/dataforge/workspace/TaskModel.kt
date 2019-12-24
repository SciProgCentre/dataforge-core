/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.workspace

import hep.dataforge.data.DataFilter
import hep.dataforge.data.DataTree
import hep.dataforge.data.DataTreeBuilder
import hep.dataforge.meta.*
import hep.dataforge.names.EmptyName
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.names.toName
import hep.dataforge.workspace.TaskModel.Companion.MODEL_TARGET_KEY


/**
 * A model for task execution
 * @param name the name of the task
 * @param meta the meta for the task (not for the whole configuration)
 * @param dependencies a list of direct dependencies for this task
 */
data class TaskModel(
    val name: Name,
    val meta: Meta,
    val dependencies: Collection<Dependency>
) : MetaRepr {
    //TODO provide a way to get task descriptor
    //TODO add pre-run check of task result type?

    override fun toMeta(): Meta = buildMeta {
        "name" put name.toString()
        "meta" put meta
        "dependsOn" put {
            val dataDependencies = dependencies.filterIsInstance<DataDependency>()
            val taskDependencies = dependencies.filterIsInstance<TaskDependency<*>>()
            setIndexed("data".toName(), dataDependencies.map { it.toMeta() })
            setIndexed("task".toName(), taskDependencies.map { it.toMeta() }) { taskDependencies[it].name.toString() }
            //TODO ensure all dependencies are listed
        }
    }

    companion object {
        val MODEL_TARGET_KEY = "@target".asName()
    }
}

/**
 * Build input for the task
 */
fun TaskModel.buildInput(workspace: Workspace): DataTree<Any> {
    return DataTreeBuilder(Any::class).apply {
        dependencies.forEach { dep ->
            update(dep.apply(workspace))
        }
    }.build()
}

interface TaskDependencyContainer {
    val defaultMeta: Meta
    fun add(dependency: Dependency)
}

/**
 * Add dependency for a task defined in a workspace and resolved by
 */
fun TaskDependencyContainer.dependsOn(
    name: Name,
    placement: Name = EmptyName,
    meta: Meta = defaultMeta
): WorkspaceTaskDependency =
    WorkspaceTaskDependency(name, meta, placement).also { add(it) }

fun TaskDependencyContainer.dependsOn(
    name: String,
    placement: Name = EmptyName,
    meta: Meta = defaultMeta
): WorkspaceTaskDependency =
    dependsOn(name.toName(), placement, meta)

fun <T : Any> TaskDependencyContainer.dependsOn(
    task: Task<T>,
    placement: Name = EmptyName,
    meta: Meta = defaultMeta
): DirectTaskDependency<T> =
    DirectTaskDependency(task, meta, placement).also { add(it) }

fun <T : Any> TaskDependencyContainer.dependsOn(
    task: Task<T>,
    placement: String,
    meta: Meta = defaultMeta
): DirectTaskDependency<T> =
    DirectTaskDependency(task, meta, placement.toName()).also { add(it) }

fun <T : Any> TaskDependencyContainer.dependsOn(
    task: Task<T>,
    placement: Name = EmptyName,
    metaBuilder: MetaBuilder.() -> Unit
): DirectTaskDependency<T> =
    dependsOn(task, placement, buildMeta(metaBuilder))

/**
 * Add custom data dependency
 */
fun TaskDependencyContainer.data(action: DataFilter.() -> Unit): DataDependency =
    DataDependency(DataFilter(action)).also { add(it) }

/**
 * User-friendly way to add data dependency
 */
fun TaskDependencyContainer.data(pattern: String? = null, from: String? = null, to: String? = null): DataDependency =
    data {
        pattern?.let { this.pattern = it }
        from?.let { this.from = it }
        to?.let { this.to = it }
    }

/**
 * Add all data as root node
 */
fun TaskDependencyContainer.allData(to: Name = EmptyName) = AllDataDependency(to).also { add(it) }

/**
 * A builder for [TaskModel]
 */
class TaskModelBuilder(val name: Name, meta: Meta = EmptyMeta) : TaskDependencyContainer {
    /**
     * Meta for current task. By default uses the whole input meta
     */
    var meta: MetaBuilder = meta.builder()
    val dependencies = HashSet<Dependency>()

    override val defaultMeta: Meta get() = meta

    override fun add(dependency: Dependency) {
        dependencies.add(dependency)
    }

    var target: String by this.meta.string(key = MODEL_TARGET_KEY, default = "")


    fun build(): TaskModel = TaskModel(name, meta.seal(), dependencies)
}


val TaskModel.target get() = meta[MODEL_TARGET_KEY]?.string ?: ""