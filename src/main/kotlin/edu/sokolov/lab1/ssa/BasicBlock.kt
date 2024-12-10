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
        if (pred in preds) return
        preds += pred
    }

    fun removePredecessor(pred: BasicBlock) {
        preds -= pred
    }

    operator fun plusAssign(child: Assignment) {
        childs.add(child)
    }

    operator fun minusAssign(child: Assignment) {
        childs.remove(child)
    }


    fun insert(pos: Int, node: Assignment) {
        childs.add(pos, node)
    }

    override fun toString(): String {
        return "BasicBlock(${name ?: ""} with ${childs.size} nodes)"
    }

    private var visitedMarker = false

    private fun resetMarker() {
        if (!visitedMarker) return
        visitedMarker = false
        when (val exit = exit) {
            is Exit.NoNext -> Unit
            is Exit.Ret -> Unit
            is Exit.Unconditional -> exit.next.resetMarker()
            is Exit.Conditional -> exit.trueBlock.resetMarker().also { exit.falseBlock.resetMarker() }
        }
    }

    override fun hashCode(): Int {
        return hashCode(hashMapOf()).also { resetMarker() }
    }

    private fun localHash(): Int {
        return Objects.hash(*childs.toTypedArray())
    }

    private fun hashCode(visited: HashMap<BasicBlock, Int>): Int {
        if (visitedMarker) return localHash()
        visitedMarker = true
        val hash = Objects.hash(localHash(), exitHash(visited))
        return hash
    }

    private fun exitHash(visited: HashMap<BasicBlock, Int> = hashMapOf()) = when (val exit = exit) {
        is Exit.NoNext -> exit.hashCode()
        is Exit.Unconditional -> exit.next.hashCode(visited)
        is Exit.Conditional -> Objects.hash(exit.trueBlock.hashCode(visited), exit.falseBlock.hashCode(visited), exit.cond.hashCode())
        is Exit.Ret -> exit.ret.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BasicBlock
        if (childs.size != other.childs.size) return false
        if (childs.zip(other.childs).any { (c1, c2) -> c1 != c2 }) return false
        return hashCode() == other.hashCode()
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