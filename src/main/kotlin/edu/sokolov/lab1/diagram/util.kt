package edu.sokolov.lab1.diagram

import edu.sokolov.lab1.ssa.BasicBlock

private const val NEW_LINE = "<br>"
private const val DOUBLE_QUOTE = "#quot;"

private var bbcounter = 0


fun BasicBlock.toDiagramBuilder(visited: HashMap<BasicBlock, DNode> = hashMapOf(), builder: DiagramBuilder = DiagramBuilder()): DiagramBuilder {
    if (this in visited) return builder
    val id = "$name\$${bbcounter++}"

    val nodeContent = when {
        children.isEmpty() -> if (exit is BasicBlock.Exit.NoNext) "exit" else "_"
        else -> nodesToString()
    }
    val node = DNode(content = nodeContent, id = id)
    visited[this] = node
    builder.node { node }

    when (val exit = exit) {
        is BasicBlock.Exit.NoNext -> Unit
        is BasicBlock.Exit.Ret -> builder.node { DNode(content = "return ${exit.ret}", id = "ret\$${bbcounter++}").also { builder.edge { DEdge(node, it) } } }
        is BasicBlock.Exit.Unconditional -> exit.next.toDiagramBuilder(visited, builder).also { builder.edge { DEdge(node, visited[exit.next]!!) } }
        is BasicBlock.Exit.Conditional -> {
            val cond = DNode(content = exit.cond.toString(), id = "cond\$${bbcounter++}", kind = NodeShape.Diamond)
            val trueNode = exit.trueBlock.toDiagramBuilder(visited, builder).let { visited[exit.trueBlock]!! }
            val falseNode = exit.falseBlock.toDiagramBuilder(visited, builder).let { visited[exit.falseBlock]!! }
            builder.node { cond }
            builder.edge { DEdge(node, cond) }
            builder.edge { DEdge(cond, trueNode, label = "true") }
            builder.edge { DEdge(cond, falseNode, label = "false") }
        }
    }

    return builder
}

private fun BasicBlock.nodesToString(): String {
    val builder = StringBuilder()

    for (idx in children.indices) {
        val node = children[idx]
        builder.append(node)
        if (idx != children.lastIndex) builder.append(NEW_LINE)
    }
    return builder.toString()
}

fun String.escape() = this.replace("\n", NEW_LINE).replace("\"", DOUBLE_QUOTE)