package edu.sokolov.lab1.ssa

sealed interface Expr : Node {
    data object Undefined : Expr
}

data class BinaryExpr(val left: Expr, val op: String, val right: Expr) : Expr {
    override fun toString(): String {
        return "($left $op $right)"
    }
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

class PhiExpr private constructor(
    private val preds: ArrayList<Definition.Stamp>,
    val ofName: String
) : Expr {
    constructor(predecessors: List<Definition.Stamp> = emptyList()) : this(
        ArrayList(predecessors),
        predecessors.firstOrNull()?.definition?.name ?: ""
    )

    constructor(vararg predecessors: Definition.Stamp) : this(
        ArrayList(predecessors.toList()),
        predecessors.firstOrNull()?.definition?.name ?: ""
    )

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
        return "φ(${preds.joinToString()})"
    }

    companion object {
        val Incomplete: PhiExpr
            get() = PhiExpr(arrayListOf(), "")

        fun Incomplete(ofName: String) = PhiExpr(arrayListOf(), ofName)
    }
}
