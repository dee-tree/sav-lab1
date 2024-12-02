package edu.sokolov.lab1.opt

import edu.sokolov.lab1.ssa.BasicBlock
import edu.sokolov.lab1.ssa.BinaryExpr
import edu.sokolov.lab1.ssa.CallExpr
import edu.sokolov.lab1.ssa.ConstExpr
import edu.sokolov.lab1.ssa.Definition
import edu.sokolov.lab1.ssa.Expr
import edu.sokolov.lab1.ssa.MemberCallExpr
import edu.sokolov.lab1.ssa.NamedArgument
import edu.sokolov.lab1.ssa.PhiExpr

/**
 * @return index of [of] usage in basic block [this]
 */
fun BasicBlock.findUsage(of: Definition.Stamp, onRange: IntRange = children.indices): Int? {
    for (i in onRange) {
        if (of in children[i].rhs) return i
    }
    return null
}

fun BasicBlock.findUsage(of: Definition.Stamp, fromIdx: Int): Int? = findUsage(of, fromIdx..children.lastIndex)

fun BasicBlock.usages(of: Definition.Stamp): List<Int> {
    val res = arrayListOf<Int>()
    var idx = 0
    while (true) {
        idx = findUsage(of, idx) ?: break
        res += idx
        idx++
    }
    return res
}

operator fun Expr.contains(what: Definition.Stamp): Boolean {
    return when (this) {
        is Definition.Stamp -> what == this
        is BinaryExpr -> what in left || what in right
        is PhiExpr -> what in predecessors
        is ConstExpr<*> -> false
        is MemberCallExpr -> what in base || args.any { what in it }
        is CallExpr -> what in callee || args.any { what in it }
        is Expr.Undefined -> false
        is NamedArgument<*> -> what in value
    }
}

fun BasicBlock.findConst(): Int? {
    for (i in children.indices) {
        if (children[i].rhs is ConstExpr<*>) return i
    }
    return null
}

fun Expr.substitute(before: Expr, after: Expr): Expr {
    return when (this) {
        is Definition.Stamp -> if (this == before) after else this
        is BinaryExpr -> {
            val newLeft = left.substitute(before, after)
            val newRight = right.substitute(before, after)
            BinaryExpr(newLeft, op, newRight)
        }
        is PhiExpr -> {
            if (before !in predecessors) return this
            if (after !is Definition.Stamp) return this
            PhiExpr(predecessors.map { it.substitute(before, after) as Definition.Stamp })
        }
        is ConstExpr<*> -> if (this == before) after else this
        is MemberCallExpr -> {
            val newBase = base.substitute(before, after)
            val newArgs = args.map { it.substitute(before, after) }
            MemberCallExpr(newBase, method, newArgs)
        }
        is CallExpr -> {
            val newBase = callee.substitute(before, after)
            val newArgs = args.map { it.substitute(before, after) }
            CallExpr(newBase, newArgs)
        }
        is Expr.Undefined -> this
        is NamedArgument<*> -> NamedArgument(name = name, value = value.substitute(before, after))
    }
}

fun <A :Expr, B: Expr> Expr.substitute(rule: HashMap<A, B>): Expr {
    var newExpr = this
    for ((before, after) in rule) {
        newExpr = newExpr.substitute(before, after)
    }
    return newExpr
}

fun BasicBlock.substituteUsages(rule: HashMap<Expr, Expr>): BasicBlock {
    val newBB = BasicBlock().also {  predecessors.forEach { pred -> it.addPredecessor(pred) } }

    for (child in children) {
        newBB += child.copy(rhs = child.rhs.substitute(rule))
    }

    when (exit) {
        is BasicBlock.Exit.NoNext -> newBB.exit = BasicBlock.Exit.NoNext
        is BasicBlock.Exit.Ret -> {
            val exit = exit as BasicBlock.Exit.Ret
            newBB.exit = if (exit.ret in rule) BasicBlock.Exit.Ret(exit.ret.substitute(exit.ret, rule[exit.ret]!!)) else exit
        }
        is BasicBlock.Exit.Unconditional -> newBB.exit = BasicBlock.Exit.Unconditional(exit.next!!.substituteUsages(rule))
        is BasicBlock.Exit.Conditional -> {
            val exit = exit as BasicBlock.Exit.Conditional
            val newCond = rule[exit.cond] ?: exit.cond
            newBB.exit = BasicBlock.Exit.Conditional(newCond, exit.trueBlock.substituteUsages(rule), exit.falseBlock.substituteUsages(rule))
        }
    }

    return newBB
}