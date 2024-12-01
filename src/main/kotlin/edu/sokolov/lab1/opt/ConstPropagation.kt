package edu.sokolov.lab1.opt

import edu.sokolov.lab1.ssa.BasicBlock
import edu.sokolov.lab1.ssa.ConstExpr
import edu.sokolov.lab1.ssa.Definition

fun constPropagation(bb: BasicBlock, rules: HashMap<Definition.Stamp, ConstExpr<*>> = hashMapOf(), eliminateDefinitions: Boolean = false): BasicBlock {
    val newBB = BasicBlock()
    bb.predecessors.forEach { pred -> newBB.addPredecessor(pred) }
    val consts = hashMapOf<Definition.Stamp, ConstExpr<*>>().apply { putAll(rules) }

    for (instrIdx in bb.children.indices) {
        val assignment = bb.children[instrIdx]

        when {
            assignment.rhs is ConstExpr<*> -> {
                consts[assignment.lhs] = assignment.rhs
                if (!eliminateDefinitions) newBB += assignment
            }
            assignment.rhs in consts -> {
                consts[assignment.lhs] = consts[assignment.rhs]!!
                if (!eliminateDefinitions) newBB += assignment.copy(rhs = consts[assignment.rhs]!!)
            }
            else -> newBB += assignment.copy(rhs = assignment.rhs.substitute(consts))
        }
    }

    when (bb.exit) {
        is BasicBlock.Exit.NoNext -> newBB.exit = BasicBlock.Exit.NoNext
        is BasicBlock.Exit.Ret -> {
            val exit = bb.exit as BasicBlock.Exit.Ret
            newBB.exit = if (exit.ret in consts) BasicBlock.Exit.Ret(exit.ret.substitute(exit.ret, consts[exit.ret]!!)) else exit
        }
        is BasicBlock.Exit.Unconditional -> newBB.exit = BasicBlock.Exit.Unconditional(constPropagation(bb.exit.next!!, consts))
        is BasicBlock.Exit.Conditional -> {
            val exit = bb.exit as BasicBlock.Exit.Conditional
            val newCond = consts[exit.cond] ?: exit.cond
            newBB.exit = BasicBlock.Exit.Conditional(newCond, constPropagation(exit.trueBlock, consts), constPropagation(exit.falseBlock, consts))
        }
    }
    return newBB
}