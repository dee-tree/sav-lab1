package edu.sokolov.lab1.ssa

class BasicBlock(val children: List<Assignment>, val exit: Exit): Statement {
    val next: Statement? get() = exit.next

    sealed class Exit(open val cond: Expr?, open val next: BasicBlock?) {
        data object NoNext: Exit(null, null)
        data class Unconditional(override val next: BasicBlock) : Exit(null, next)
//        data class Conditional(override val cond: Expr, override val next: BasicBlock) : Exit(cond, next)
        data class Conditional(override val cond: Expr, val trueBlock: BasicBlock, val falseBlock: BasicBlock) : Exit(cond, null)
    }
}