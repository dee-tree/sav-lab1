package edu.sokolov.lab1.diagram

import java.io.File

class DiagramBuilder(val name: String = "Diagram") {
//    private val diagramKind = "graph TD"
    private val diagramKind = "flowchart TD"

    private val nodes = hashSetOf<DNode>()
    private val edges = hashSetOf<DEdge>()

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
        builder.appendLine("$diagramKind")
        nodes.forEach { n -> builder.append('\t'); builder.appendLine(n) }
        edges.forEach { e -> builder.append('\t'); builder.appendLine(e) }
        return builder.toString()
    }
}