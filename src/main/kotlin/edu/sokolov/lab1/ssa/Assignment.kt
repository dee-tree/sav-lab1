package edu.sokolov.lab1.ssa

data class Assignment(val lhs: Definition.Stamp, val rhs: Expr, val isIntroduction: Boolean = false) : Statement {
    companion object {
        fun introduce(lhs: Definition.Stamp, rhs: Expr = Expr.Undefined) = Assignment(lhs, rhs, true)
    }

    override fun toString(): String = "$lhs = $rhs"
}