package edu.sokolov.lab1.diagram

import java.io.File

class DiagramBuilder(val name: String = "Diagram", val alignment: String = "left", var title: String? = null) {
//    private val diagramKind = "graph TD"
    private val diagramKind = "flowchart TD"

    private val nodes = hashSetOf<DNode>()
    private val edges = hashSetOf<DEdge>()

    private val styleName = "defstyle"

    fun title(title: String): DiagramBuilder {
        this.title = title
        return this
    }

    fun edge(edge: () -> DEdge) {
        val e = edge()
        nodes += e.from
        nodes += e.to
        edges += e
    }

    fun node(node: () -> DNode) {
        val n = node()
        nodes += n
    }

    fun toFile(file: File) {
        file.writeText(toString())
    }

    override fun toString(): String {
        val builder = StringBuilder()
//        builder.appendLine("$diagramKind $name")

        title?.let {
            builder.appendLine("---")
            builder.appendLine("title: $it")
            builder.appendLine("---")
        }

        builder.appendLine(diagramKind)
        nodes.forEach { n -> builder.append('\t'); builder.appendLine("$n:::$styleName") }
        edges.forEach { e -> builder.append('\t'); builder.appendLine(e) }
        builder.appendLine("classDef $styleName text-align: $alignment;")
        return builder.toString()


    }
}