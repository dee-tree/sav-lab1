package edu.sokolov.lab1


import edu.sokolov.lab1.ssa.Transformer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile

val code = """
fun hello() {
    val h = "hello"
    val w = "wo" + "rld"
    return h + w
}
""".trimIndent()


fun main() {
    println("Hello")

    val project = KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(),
        CompilerConfiguration(),
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    ).project


    val ktfile = PsiManager.getInstance(project).findFile(LightVirtualFile("code.kt", code)) as KtFile

    val transformer = Transformer()
    ktfile.declarations.forEach(transformer::transform)
    ktfile.declarations.forEach { println(it::class.simpleName) }

    println(ktfile)
}
