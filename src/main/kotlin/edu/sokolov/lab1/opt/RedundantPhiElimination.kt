package edu.sokolov.lab1.opt

import edu.sokolov.lab1.ssa.BasicBlock
import edu.sokolov.lab1.ssa.Definition
import edu.sokolov.lab1.ssa.PhiExpr

fun eliminatePhis(bb: BasicBlock): BasicBlock {
    val rules = hashMapOf<Definition.Stamp, Definition.Stamp>()
    val visited = hashMapOf<BasicBlock, BasicBlock>()
    collectReplacements(bb, rules)

    return eliminateRedundantPhi(bb, rules, visited)
}


fun eliminateRedundantPhi(
    bb: BasicBlock,
    rules: HashMap<Definition.Stamp, Definition.Stamp> = hashMapOf(),
    visited: HashMap<BasicBlock, BasicBlock> = hashMapOf(),
): BasicBlock {
    if (bb in visited) return visited[bb]!!
    val newBB = BasicBlock(name = bb.name)
    bb.predecessors.forEach { pred -> newBB.addPredecessor(pred) }
    visited[bb] = newBB

    for (asg in bb.children) {
        if (asg.rhs is PhiExpr && asg.rhs.predecessors.size == 1) {
            continue
        } else {
            val rhs = asg.rhs.substitute(rules)
            if (rhs is PhiExpr && rhs.predecessors.size == 1) {
                continue
            } else {
                newBB += asg.copy(rhs = rhs)
            }

        }
    }

    when (bb.exit) {
        is BasicBlock.Exit.NoNext -> newBB.exit = BasicBlock.Exit.NoNext
        is BasicBlock.Exit.Ret -> {
            val exit = bb.exit as BasicBlock.Exit.Ret
            newBB.exit = if (exit.ret in rules) BasicBlock.Exit.Ret(exit.ret.substitute(rules)) else exit
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
            newBB.exit = BasicBlock.Exit.Conditional(newCond, newtrue, newfalse)
        }
    }

    return newBB

}

fun collectReplacements(
    bb: BasicBlock,
    rules: HashMap<Definition.Stamp, Definition.Stamp> = hashMapOf(),
    visited: HashSet<BasicBlock> = hashSetOf()
) {
    if (bb in visited) return
    visited += bb

    for (asg in bb.children) {
        if (asg.rhs is PhiExpr && asg.rhs.predecessors.size == 1) {
            val rule = asg.rhs.predecessors.first()
            if (rule in rules) {
                rules[asg.lhs] = rules[rule]!!
            } else {
                rules[asg.lhs] = rule
            }
        } else {
            val rhs = asg.rhs.substitute(rules)
            if (rhs is PhiExpr && rhs.predecessors.size == 1) {
                val rule = rhs.predecessors.first()
                if (rule in rules) {
                    rules[asg.lhs] = rules[rule]!!
                } else {
                    rules[asg.lhs] = rule
                }
            }
        }
    }

    when (val exit = bb.exit) {
        is BasicBlock.Exit.NoNext -> Unit
        is BasicBlock.Exit.Ret -> Unit
        is BasicBlock.Exit.Unconditional -> {
            collectReplacements(bb.exit.next!!, rules, visited)
        }

        is BasicBlock.Exit.Conditional -> {
            collectReplacements(exit.trueBlock, rules, visited)
            collectReplacements(exit.falseBlock, rules, visited)
        }
    }
}