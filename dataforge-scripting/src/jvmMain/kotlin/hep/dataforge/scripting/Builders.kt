package hep.dataforge.scripting

import hep.dataforge.context.Global
import hep.dataforge.workspace.Workspace
import hep.dataforge.workspace.WorkspaceBuilder
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

object Builders {

    fun buildWorkspace(source: SourceCode): Workspace {
        val builder = WorkspaceBuilder(Global)

        val workspaceScriptConfiguration = ScriptCompilationConfiguration {
            baseClass(Any::class)
            implicitReceivers(WorkspaceBuilder::class)
            jvm{
                dependenciesFromCurrentContext()
            }
        }

        val evaluationConfiguration = ScriptEvaluationConfiguration {
            implicitReceivers(builder)
        }

        val res = BasicJvmScriptingHost().eval(source, workspaceScriptConfiguration, evaluationConfiguration)
        res.reports.forEach{ scriptDiagnostic ->
            scriptDiagnostic.exception?.let { throw it }
        }

        return builder.build()
    }

    fun buildWorkspace(file: File): Workspace = buildWorkspace(file.toScriptSource())

    fun buildWorkspace(string: String): Workspace = buildWorkspace(string.toScriptSource())
}