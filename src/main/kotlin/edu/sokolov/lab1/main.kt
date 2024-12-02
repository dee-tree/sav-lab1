package edu.sokolov.lab1


import edu.sokolov.lab1.diagram.toDiagramBuilder
import edu.sokolov.lab1.opt.constPropagation
import edu.sokolov.lab1.opt.eliminateRedundantPhi
import edu.sokolov.lab1.opt.tmpPropagation
import edu.sokolov.lab1.ssa.Transformer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import kotlin.math.tan

//val code = """
//fun hello() {
//    val h = "hello"
//    val w = "wo" + "rld"
//    return h + w
//}
//""".trimIndent()

/*val code = """
fun hello() {
    var h = 35
    h = h + 2
    val w = 47 + 31
    
    return h + w
}
""".trimIndent()*/

//val code = """
//fun hello() {
//    var h = 35
//    var g = 34
//    if (h > g) g++ else h++
//
//    return h + g
//}
//""".trimIndent()

// String interpolation
/*val code = """
fun hello() {
    var h = 0
    var g = 1
    g++
    val str = "prefix:${'$'}h and ${'$'}{g} suffix"
}
""".trimIndent()*/

// For loop
val code = """
fun hello() {
    val list = listOf(1, 2, 3 + 4, last = 5)
    for (i in list) {
        println(i)
    }
}
""".trimIndent()


fun main() {
    val diagramFileName = "ssa-diagram"
    val diagramFileExt = "txt"

    println("Hello")

    val project = KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(),
        CompilerConfiguration(),
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    ).project

    val ktfile = PsiManager.getInstance(project).findFile(LightVirtualFile("code.kt", code)) as KtFile

    val transformer = Transformer()
    ktfile.declarations.forEach(transformer::transform)

    val bb = transformer.transform(ktfile.declarations.first())

    val propagated = constPropagation(bb)
    val phiEliminated = eliminateRedundantPhi(propagated)
    val tmpEliminated = tmpPropagation(transformer.ctx, phiEliminated)

    File("${diagramFileName}_unoptimized.$diagramFileExt").also { file ->
        bb.toDiagramBuilder().toFile(file)
    }

    File("${diagramFileName}_const-prop.$diagramFileExt").also { file ->
        propagated.toDiagramBuilder().toFile(file)
    }

    File("${diagramFileName}_phi-elim.$diagramFileExt").also { file ->
        phiEliminated.toDiagramBuilder().toFile(file)
    }

    File("${diagramFileName}.$diagramFileExt").also { file ->
        tmpEliminated.toDiagramBuilder().toFile(file)
    }

}
