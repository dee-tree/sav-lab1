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

// If blocks
/*val code = """
fun hello() {
    val list = listOf(1, 2, 3 + 4, last = 5)
    if (list.isEmpty()) {
        val blist = list + 0
        if (blist != list) println("Different!") else println("Same")
        
        val size = if (blist != list) list.size else blist.size
        System.out.println(size)
    }
}
""".trimIndent()*/

// While loop
//val code = """
//fun hello() {
//    var v = 35 + 1
//    while (v > 0) {
//        v--
//        print(v)
//    }
//}
//""".trimIndent()

// For loop
//val code = """
//fun hello() {
//    val list = listOf(1, 2, 3 + 4, last = 5)
//    for (i in list) {
//        println(i)
//    }
//}
//""".trimIndent()


// Big code
/*val code = """
fun hello() {
    val list = listOf(1, 2, 3 + 4, last = 5)
    for (i in list) {
        println(i)
    }
    

}
""".trimIndent()*/

// Statistics
val code = """
class A : B(), C(), D() {

    fun aoo() {
        val x = 35.toString()
        val y = x.toInt()
    }
    
}

open class B: C() {
    override open fun doo() {}
    open fun boo(long: Long) { p2 * p2 }
    
    private fun kek() {}
    
    val p1 = "striiiing"
    val p2 = 3333
}


open class C: D() {
     open fun coo(a: Int, B: Double, c: String) {
        System.out.println(a)
     }
}

open class D {
    val prop1: Int = 2
    val prop2: Int = prop1
    open fun doo() {
        prop1 + 1
    }
    
    fun dooo() = prop1
    
    fun gogo() {}
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
    val blocks = ktfile.declarations.map(transformer::transform)

    val bb = blocks.first()

    val propagated = constPropagation(bb)
    val phiEliminated = eliminateRedundantPhi(propagated)
    val tmpEliminated = tmpPropagation(transformer.ctx, phiEliminated)


    println("Statistics Chidamber & Kemerer")
    println(transformer.ctx.stats.joinToString("\n"))

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
