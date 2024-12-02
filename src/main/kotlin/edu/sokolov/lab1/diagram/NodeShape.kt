package edu.sokolov.lab1.diagram

sealed class NodeShape(val lbrace: String, val rbrace: String) {
    data object Rectangle : NodeShape(lbrace = "[", rbrace = "]")
    data object RoundRect : NodeShape(lbrace = "(", rbrace = ")")
    data object Circle : NodeShape(lbrace = "((", rbrace = "))")
    data object Diamond : NodeShape(lbrace = "{", rbrace = "}")
}