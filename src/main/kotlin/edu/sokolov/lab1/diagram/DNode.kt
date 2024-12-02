package edu.sokolov.lab1.diagram

data class DNode(val content: String, val id: String, val kind: NodeShape = NodeShape.RoundRect) {
    override fun toString(): String = "$id${kind.lbrace}$safeContent${kind.rbrace}"

    private val safeContent: String
        get() = "\"${content.replace("\"", "\\\"")}\""
}