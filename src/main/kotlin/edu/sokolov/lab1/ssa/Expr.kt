package edu.sokolov.lab1.ssa

import java.util.HashMap
import java.util.HashSet

sealed interface Expr : Node {
    data object Undefined : Expr
}

data class BinaryExpr(val left: Expr, val op: String, val right: Expr) : Expr {
    override fun toString(): String {
        return "($left $op $right)"
    }
}

data class BreakExpr(val label: String? = null) : Expr {

}

data class ContinueExpr(val label: String? = null): Expr {

}

data class NamedArgument<T : Expr>(val name: String, val value: T) : Expr {
    override fun toString(): String = "$name=$value"
}

data class CallExpr(val callee: Expr, val args: List<Expr> = emptyList()) : Expr {
    override fun toString(): String = "$callee(${args.joinToString()})"
}
data class MemberCallExpr(val base: Expr, val method: String, val args: List<Expr> = emptyList()) : Expr {
    override fun toString(): String = "$base.$method(${args.joinToString()})"
}

sealed class ConstExpr<T>(open val value: T) : Expr {
    data class BoolLiteral(override val value: Boolean) : ConstExpr<Boolean>(value) {
        override fun toString(): String = "$value"
    }

    data class IntLiteral(override val value: Int) : ConstExpr<Int>(value) {
        override fun toString(): String = "$value"
    }
    data class UIntLiteral(override val value: UInt) : ConstExpr<UInt>(value) {
        override fun toString(): String = "{$value}U"
    }
    data class LongLiteral(override val value: Long) : ConstExpr<Long>(value) {
        override fun toString(): String = "${value}L"
    }
    data class ULongLiteral(override val value: ULong) : ConstExpr<ULong>(value) {
        override fun toString(): String = "${value}UL"
    }

    data class FloatLiteral(override val value: Float) : ConstExpr<Float>(value) {
        override fun toString(): String = "${value}f"
    }
    data class DoubleLiteral(override val value: Double) : ConstExpr<Double>(value) {
        override fun toString(): String = "$value"
    }

    data class CharLiteral(override val value: Char) : ConstExpr<Char>(value) {
        override fun toString(): String = "'$value'"
    }

    data class StringLiteral(override val value: String) : ConstExpr<String>(value) {
        override fun toString(): String = "\"$value\""
    }
    data object NullLiteral : ConstExpr<Unit>(Unit) {
        override fun toString(): String = "null"
    }
}

data class PhiExpr private constructor(
    private val preds: HashSet<Definition.Stamp>,
    val ofName: String
) : Expr {
    constructor(predecessors: Set<Definition.Stamp> = emptySet()) : this(
        HashSet(predecessors),
        predecessors.firstOrNull()?.definition?.name ?: ""
    )

    constructor(vararg predecessors: Definition.Stamp) : this(
        HashSet(predecessors.toSet()),
        predecessors.firstOrNull()?.definition?.name ?: ""
    )

    val predecessors: Set<Definition.Stamp>
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

    operator fun minusAssign(predecessor: Definition.Stamp) {
        preds -= predecessor
    }

    override fun toString(): String {
        return "φ(${preds.joinToString()})"
    }

    companion object {
        val Incomplete: PhiExpr
            get() = PhiExpr(hashSetOf(), "")

        fun Incomplete(ofName: String) = PhiExpr(hashSetOf(), ofName)
    }
}
