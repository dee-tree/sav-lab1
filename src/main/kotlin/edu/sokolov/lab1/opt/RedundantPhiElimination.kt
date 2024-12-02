package edu.sokolov.lab1.opt

import edu.sokolov.lab1.ssa.BasicBlock
import edu.sokolov.lab1.ssa.Definition
import edu.sokolov.lab1.ssa.PhiExpr

fun eliminateRedundantPhi(
    bb: BasicBlock,
    rules: HashMap<Definition.Stamp, Definition.Stamp> = hashMapOf(),
    visited: HashMap<BasicBlock, BasicBlock> = hashMapOf()
): BasicBlock {
    if (bb in visited) return visited[bb]!!
    val newBB = BasicBlock(name = bb.name)
    bb.predecessors.forEach { pred -> newBB.addPredecessor(pred) }
    visited[bb] = newBB

    for (asg in bb.children) {
        if (asg.rhs is PhiExpr && asg.rhs.predecessors.size == 1) {
            rules[asg.lhs] = asg.rhs.predecessors.first()
        } else {
            newBB += asg.copy(rhs = asg.rhs.substitute(rules))
        }
    }

    when (bb.exit) {
        is BasicBlock.Exit.NoNext -> newBB.exit = BasicBlock.Exit.NoNext
        is BasicBlock.Exit.Ret -> {
            val exit = bb.exit as BasicBlock.Exit.Ret
            newBB.exit =
                if (exit.ret in rules) BasicBlock.Exit.Ret(exit.ret.substitute(exit.ret, rules[exit.ret]!!)) else exit
        }

        is BasicBlock.Exit.Unconditional -> newBB.exit =
            BasicBlock.Exit.Unconditional(eliminateRedundantPhi(bb.exit.next!!, rules, visited))

        is BasicBlock.Exit.Conditional -> {
            val exit = bb.exit as BasicBlock.Exit.Conditional
            val newCond = rules[exit.cond] ?: exit.cond
            newBB.exit = BasicBlock.Exit.Conditional(
                newCond,
                eliminateRedundantPhi(exit.trueBlock, rules, visited),
                eliminateRedundantPhi(exit.falseBlock, rules, visited)
            )
        }
    }
    return newBB

}