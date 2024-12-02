package edu.sokolov.lab1.diagram

data class DEdge(val from: DNode, val to: DNode, val label: String? = null) {
    private val arrow = "-->"
    override fun toString(): String {
        val slabel = label?.let { "|\"${it.replace("\"", "\\\"")}\"|" } ?: ""
//        return "$from $arrow$slabel $to"
        return "${from.id} $arrow $slabel ${to.id}"
    }
}