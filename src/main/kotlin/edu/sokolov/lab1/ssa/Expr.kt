package edu.sokolov.lab1.ssa

import org.jetbrains.kotlin.psi.KtExpression

sealed interface Expr : Node {
//    data class Stub(val expr: KtExpression) : Expr {
//        override fun toString(): String = expr.text
//    }

    data object Undefined : Expr
}

data class BinaryExpr(val left: Expr, val op: String, val right: Expr) : Expr {
    override fun toString(): String {
        return "($left $op $right)"
    }
}

data class NamedArgument<T : Expr>(val name: String, val value: T) : Expr

data class CallExpr(val callee: Expr, val args: List<Expr> = emptyList()) : Expr
data class MemberCallExpr(val base: Expr, val method: String, val args: List<Expr> = emptyList()) : Expr

sealed class ConstExpr<T>(open val value: T) : Expr {
    data class BoolLiteral(override val value: Boolean) : ConstExpr<Boolean>(value)

    data class IntLiteral(override val value: Int) : ConstExpr<Int>(value)
    data class UIntLiteral(override val value: UInt) : ConstExpr<UInt>(value)
    data class LongLiteral(override val value: Long) : ConstExpr<Long>(value)
    data class ULongLiteral(override val value: ULong) : ConstExpr<ULong>(value)

    data class FloatLiteral(override val value: Float) : ConstExpr<Float>(value)
    data class DoubleLiteral(override val value: Double) : ConstExpr<Double>(value)

    data class CharLiteral(override val value: Char) : ConstExpr<Char>(value)
    data class StringLiteral(override val value: String) : ConstExpr<String>(value)
    data object NullLiteral : ConstExpr<Unit>(Unit)
}

class PhiExpr private constructor(
    private val preds: ArrayList<Definition.Stamp>,
    val ofName: String
) : Expr {
    constructor(predecessors: List<Definition.Stamp> = emptyList()) : this(ArrayList(predecessors), predecessors.firstOrNull()?.definition?.name ?: "")
    constructor(vararg predecessors: Definition.Stamp) : this(ArrayList(predecessors.toList()), predecessors.firstOrNull()?.definition?.name ?: "")

    val predecessors: List<Definition.Stamp>
        get() = preds

    val isComplete: Boolean
        get() = preds.isNotEmpty()

    val isRedundant: Boolean
        get() = preds.size <= 1

    operator fun plusAssign(predecessor: Definition.Stamp) {
        preds += predecessor
    }

    operator fun plusAssign(other: PhiExpr) {
        preds += other.preds
    }

    override fun toString(): String {
        return "phi($preds)"
    }

    companion object {
        val Incomplete: PhiExpr
            get() = PhiExpr(arrayListOf(), "")

        fun Incomplete(ofName: String) = PhiExpr(arrayListOf(), ofName)
    }
}
