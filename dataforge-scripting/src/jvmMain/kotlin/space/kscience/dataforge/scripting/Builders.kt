package space.kscience.dataforge.scripting

import space.kscience.dataforge.context.*
import space.kscience.dataforge.workspace.Workspace
import space.kscience.dataforge.workspace.WorkspaceBuilder
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

public object Builders {

    private fun buildWorkspace(source: SourceCode, context: Context = Global): Workspace {
        val builder = WorkspaceBuilder(context)

        val workspaceScriptConfiguration = ScriptCompilationConfiguration {
//            baseClass(Any::class)
            implicitReceivers(WorkspaceBuilder::class)
            defaultImports(
                "space.kscience.dataforge.meta.*",
                "space.kscience.dataforge.workspace.*"
            )
            jvm {
                dependenciesFromCurrentContext(wholeClasspath = true)
            }
            hostConfiguration(defaultJvmScriptingHostConfiguration)
            compilerOptions("-jvm-target", Runtime.version().feature().toString(),"-Xcontext-receivers")
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
                    ScriptDiagnostic.Severity.INFO -> context.logger.info {  scriptDiagnostic.toString() }
                    ScriptDiagnostic.Severity.DEBUG -> context.logger.debug { scriptDiagnostic.toString() }
                }
            }
        }

        return builder.build()
    }

    public fun buildWorkspace(scriptFile: File): Workspace = buildWorkspace(scriptFile.toScriptSource())

    public fun buildWorkspace(scriptString: String): Workspace = buildWorkspace(scriptString.toScriptSource())
}