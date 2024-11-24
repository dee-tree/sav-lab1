package edu.sokolov.lab1.ssa

data class Definition(val name: String) {
    private var freshId = 0L

    fun new() = Stamp(freshId++, this)

    data class Stamp(val id: Long, val definition: Definition): Expr
}
