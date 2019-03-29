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
import hep.dataforge.names.toName
import hep.dataforge.workspace.TaskModel.Companion.MODEL_TARGET_KEY


/**
 * A model for task execution
 * @param name the name of the task
 * @param meta the meta for the task (not for the whole configuration)
 * @param dependencies a list of direct dependencies for this task
 */
data class TaskModel(
    val name: String,
    val meta: Meta,
    val dependencies: Collection<Dependency>
) : MetaRepr {
    //TODO provide a way to get task descriptor
    //TODO add pre-run check of task result type?

    override fun toMeta(): Meta = buildMeta {
        "name" to name
        "meta" to meta
        "dependsOn" to {
            val dataDependencies = dependencies.filterIsInstance<DataDependency>()
            val taskDependencies = dependencies.filterIsInstance<TaskModelDependency>()
            setIndexed("data".toName(), dataDependencies.map { it.toMeta() })
            setIndexed("task".toName(), taskDependencies.map { it.toMeta() }) { taskDependencies[it].name }
            //TODO ensure all dependencies are listed
        }
    }

    companion object {
        const val MODEL_TARGET_KEY = "@target"
    }
}

/**
 * Build input for the task
 */
fun TaskModel.buildInput(workspace: Workspace): DataTree<Any> {
    return DataTreeBuilder(Any::class).apply {
        dependencies.asSequence().flatMap { it.apply(workspace).data() }.forEach { (name, data) ->
            //TODO add concise error on replacement
            this[name] = data
        }
    }.build()
}

/**
 * A builder for [TaskModel]
 */
class TaskModelBuilder(val name: String, meta: Meta = EmptyMeta) {
    /**
     * Meta for current task. By default uses the whole input meta
     */
    var meta: MetaBuilder = meta.builder()
    val dependencies = HashSet<Dependency>()

    var target: String by this.meta.string(key = MODEL_TARGET_KEY, default = "")

    /**
     * Add dependency for
     */
    fun dependsOn(name: String, meta: Meta = this.meta, placement: Name = EmptyName) {
        dependencies.add(TaskModelDependency(name, meta, placement))
    }

    fun dependsOn(task: Task<*>, meta: Meta = this.meta, placement: Name = EmptyName) =
        dependsOn(task.name, meta, placement)

    fun dependsOn(task: Task<*>, placement: Name = EmptyName, metaBuilder: MetaBuilder.() -> Unit) =
        dependsOn(task.name, buildMeta(metaBuilder), placement)

    /**
     * Add custom data dependency
     */
    fun data(action: DataFilter.() -> Unit) {
        dependencies.add(DataDependency(DataFilter.build(action)))
    }

    /**
     * User-friendly way to add data dependency
     */
    fun data(pattern: String? = null, from: String? = null, to: String? = null) = data {
        pattern?.let { this.pattern = it }
        from?.let { this.from = it }
        to?.let { this.to = it }
    }

    /**
     * Add all data as root node
     */
    fun allData(to: Name = EmptyName) {
        dependencies.add(AllDataDependency(to))
    }

    fun build(): TaskModel = TaskModel(name, meta.seal(), dependencies)
}


val TaskModel.target get() = meta[MODEL_TARGET_KEY]?.string ?: ""