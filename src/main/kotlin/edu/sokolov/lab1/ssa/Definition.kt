package edu.sokolov.lab1.ssa

data class Definition(val name: String) {
    private var freshId = 0L

    fun new() = Stamp(freshId++, this)

    open class Stamp(val id: Long, val definition: Definition): Expr {
        override fun toString(): String {
            return "Stamp(${definition.name}_$id)"
        }
    }

    class Incomplete(definition: Definition) : Stamp(0L, definition) {
        override fun toString(): String { return "Incomplete($definition)" }
    }
}
