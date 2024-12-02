package edu.sokolov.lab1.ssa

import java.util.Objects

class BasicBlock(next: Exit = Exit.NoNext, val name: String? = null) : Statement {
    private val childs = ArrayList<Assignment>()
    val children: List<Assignment>
        get() = childs

    private var _exit: Exit = next

    var exit: Exit
        get() = _exit
        set(value) {
            _exit = value
        }

    private val preds = arrayListOf<BasicBlock>()

    val predecessors: List<BasicBlock>
        get() = preds

    fun addPredecessor(pred: BasicBlock) {
        preds += pred
    }

    operator fun plusAssign(child: Assignment) {
        childs.add(child)
    }

    fun insert(pos: Int, node: Assignment) {
        childs.add(pos, node)
    }

    override fun toString(): String {
        return "BasicBlock(${name ?: ""} with ${childs.size} nodes)"
    }

    override fun hashCode(): Int {
        return Objects.hash(exitHash(), *childs.toTypedArray())
    }

    private fun exitHash() = when (exit) {
        is Exit.NoNext -> 10
        is Exit.Unconditional -> 100
        is Exit.Conditional -> 1_000
        is Exit.Ret -> 10_000
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BasicBlock
        if (exitHash() != other.exitHash()) return false
        if (childs.size != other.childs.size) return false
        return childs.zip(other.childs).all { (c1, c2) -> c1 == c2 }
    }

    sealed class Exit(open val cond: Expr?, open val next: BasicBlock?) {
        data object NoNext : Exit(null, null)
        data class Unconditional(override val next: BasicBlock) : Exit(null, next)
        data class Ret(val ret: Expr) : Exit(null, null)
        data class Conditional(
            override val cond: Expr,
            val trueBlock: BasicBlock,
            val falseBlock: BasicBlock
        ) : Exit(cond, null)
    }
}