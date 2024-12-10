package edu.sokolov.lab1


import edu.sokolov.lab1.diagram.toDiagramBuilder
import edu.sokolov.lab1.opt.constPropagation
import edu.sokolov.lab1.opt.eliminateEmptyBlocks
import edu.sokolov.lab1.opt.eliminatePhis
import edu.sokolov.lab1.opt.inlineDefinitions
import edu.sokolov.lab1.ssa.Transformer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import java.io.File


fun main() {
    val case =
//        "ifs"
//        "while-loop"
//        "while-break-loop"
//        "while-continue-loop"
        "while-complex-loop"

    val codeFileName = "code.kt"

    val diagramFileName = "ssa-diagram"
    val diagramFileExt = "mmd"

    println("Hello, running $case case")

    val caseFolder = File("res/${case}")
    val project = KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(),
        CompilerConfiguration(),
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    ).project

    val codeFile = File(caseFolder, codeFileName)
    val code = codeFile.readText()

    val ktfile = PsiManager.getInstance(project).findFile(LightVirtualFile("code.kt", code)) as KtFile

    val transformer = Transformer()
    val blocks = ktfile.declarations.map(transformer::transform)

    val bb = blocks.first()

    val phiEliminated = eliminatePhis(bb)
    val inlined = inlineDefinitions(phiEliminated)
    val propagated = constPropagation(inlined, eliminateDefinitions = false)
    val withoutEmptyBlocks = eliminateEmptyBlocks(propagated)

    println("Statistics Chidamber & Kemerer")
    println(transformer.ctx.stats.joinToString("\n"))

    File(caseFolder, "${diagramFileName}_unoptimized.$diagramFileExt").also { file ->
        bb.toDiagramBuilder().title(case).toFile(file)
    }

    File(caseFolder, "${diagramFileName}_const-prop.$diagramFileExt").also { file ->
        propagated.toDiagramBuilder().title(case).toFile(file)
    }

    File(caseFolder, "${diagramFileName}_phi-elim.$diagramFileExt").also { file ->
        phiEliminated.toDiagramBuilder().title(case).toFile(file)
    }

    File(caseFolder, "${diagramFileName}_inlined.$diagramFileExt").also { file ->
        inlined.toDiagramBuilder().title(case).toFile(file)
    }

    File(caseFolder, "${diagramFileName}.$diagramFileExt").also { file ->
        withoutEmptyBlocks.toDiagramBuilder().title(case).toFile(file)
    }

}
