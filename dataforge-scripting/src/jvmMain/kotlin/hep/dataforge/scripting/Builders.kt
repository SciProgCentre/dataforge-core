package hep.dataforge.scripting

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.workspace.SimpleWorkspaceBuilder
import hep.dataforge.workspace.Workspace
import hep.dataforge.workspace.WorkspaceBuilder
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

object Builders {

    fun buildWorkspace(source: SourceCode, context: Context = Global): Workspace {
        val builder = SimpleWorkspaceBuilder(context)

        val workspaceScriptConfiguration = ScriptCompilationConfiguration {
            baseClass(Any::class)
            implicitReceivers(WorkspaceBuilder::class)
            defaultImports("hep.dataforge.workspace.*")
            jvm {
                dependenciesFromCurrentContext(wholeClasspath = true)
            }
            hostConfiguration(defaultJvmScriptingHostConfiguration)
        }

        val evaluationConfiguration = ScriptEvaluationConfiguration {
            implicitReceivers(builder)
        }

        BasicJvmScriptingHost().eval(source, workspaceScriptConfiguration, evaluationConfiguration).onFailure {
            it.reports.forEach { scriptDiagnostic ->
                when (scriptDiagnostic.severity) {
                    ScriptDiagnostic.Severity.FATAL, ScriptDiagnostic.Severity.ERROR -> {
                        context.logger.error(scriptDiagnostic.exception) { scriptDiagnostic.toString() }
                        error(scriptDiagnostic.toString())
                    }
                    ScriptDiagnostic.Severity.WARNING -> context.logger.warn { scriptDiagnostic.toString() }
                    ScriptDiagnostic.Severity.INFO -> context.logger.info { scriptDiagnostic.toString() }
                    ScriptDiagnostic.Severity.DEBUG -> context.logger.debug { scriptDiagnostic.toString() }
                }
            }
        }

        return builder.build()
    }

    fun buildWorkspace(file: File): Workspace = buildWorkspace(file.toScriptSource())

    fun buildWorkspace(string: String): Workspace = buildWorkspace(string.toScriptSource())
}