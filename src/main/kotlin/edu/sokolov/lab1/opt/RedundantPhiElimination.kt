package edu.sokolov.lab1.opt

import edu.sokolov.lab1.ssa.BasicBlock
import edu.sokolov.lab1.ssa.Definition
import edu.sokolov.lab1.ssa.Expr
import edu.sokolov.lab1.ssa.PhiExpr

fun eliminateRedundantPhi(
    bb: BasicBlock,
    rules: HashMap<Definition.Stamp, Definition.Stamp> = hashMapOf(),
    visited: HashMap<BasicBlock, BasicBlock> = hashMapOf()
): BasicBlock {
    var bb = bb
    var newBB = BasicBlock(name = bb.name)

    while (true) {
        if (bb in visited) return visited[bb]!!
        newBB = BasicBlock(name = bb.name)
//        bb.predecessors.forEach { pred -> newBB.addPredecessor(pred) }
        visited[bb] = newBB

        for (asg in bb.children) {
            if (asg.rhs is PhiExpr && asg.rhs.predecessors.size == 1) {
                rules[asg.lhs] = asg.rhs.predecessors.first()
            } else {
                val rhs = asg.rhs.substitute(rules)
                if (rhs is PhiExpr && rhs.predecessors.size == 1) {
                    rules[asg.lhs] = rhs.predecessors.first()
                } else {
                    newBB += asg.copy(rhs = rhs)
                }

            }
        }

        when (bb.exit) {
            is BasicBlock.Exit.NoNext -> newBB.exit = BasicBlock.Exit.NoNext
            is BasicBlock.Exit.Ret -> {
                val exit = bb.exit as BasicBlock.Exit.Ret
                newBB.exit =
                    if (exit.ret in rules) BasicBlock.Exit.Ret(
                        exit.ret.substitute(
                            exit.ret,
                            rules[exit.ret]!!
                        )
                    ) else exit
            }

            is BasicBlock.Exit.Unconditional -> {
                val newNext = eliminateRedundantPhi(bb.exit.next!!, rules, visited)
                newNext.addPredecessor(newBB)
                newBB.exit = BasicBlock.Exit.Unconditional(newNext)
            }

            is BasicBlock.Exit.Conditional -> {
                val exit = bb.exit as BasicBlock.Exit.Conditional
                val newCond = rules[exit.cond] ?: exit.cond
                val newtrue = eliminateRedundantPhi(exit.trueBlock, rules, visited)
                val newfalse = eliminateRedundantPhi(exit.falseBlock, rules, visited)
                newtrue.addPredecessor(newBB)
                newfalse.addPredecessor(newBB)
                newBB.exit = BasicBlock.Exit.Conditional(
                    newCond,
                    newtrue,
                    newfalse
                )
            }
        }

        if (bb == newBB) break
        visited.remove(bb)
        bb = newBB
//        visited.clear()
    }

    return newBB

}