package edu.sokolov.lab1.opt

import edu.sokolov.lab1.ssa.BasicBlock
import edu.sokolov.lab1.ssa.ConstExpr
import edu.sokolov.lab1.ssa.Definition
import edu.sokolov.lab1.ssa.Expr
import edu.sokolov.lab1.ssa.PhiExpr

fun inlineDefinitions(bb: BasicBlock): BasicBlock {
    val rules = hashMapOf<Definition.Stamp, Expr>()
    collectReplacements(bb, rules)

    println("rules: $rules")

    return inlineDefinitions(bb, rules)
}

fun inlineDefinitions(
    bb: BasicBlock,
    rules: HashMap<Definition.Stamp, Expr>,
    visitedBlocks: HashMap<BasicBlock, BasicBlock> = hashMapOf(),
): BasicBlock {
    if (bb in visitedBlocks) return visitedBlocks[bb]!!
    val newBB = BasicBlock(name = bb.name)
    bb.predecessors.forEach { pred -> newBB.addPredecessor(pred) }
    visitedBlocks[bb] = newBB

    for (asg in bb.children) {
        when {
//             asg.rhs !is PhiExpr && bb.allUsages(asg.lhs) == 1 && !bb.isUsedInPhi(asg.lhs) -> {
//                 rules[asg.lhs] = asg.rhs.substitute(rules)
//            }

//            asg.rhs is ConstExpr<*> && !bb.isUsedInPhi(asg.lhs) -> Unit

            else -> newBB += asg.copy(rhs = asg.rhs.substitute(rules))
        }
    }

    when (val exit = bb.exit) {
        is BasicBlock.Exit.NoNext -> newBB.exit = BasicBlock.Exit.NoNext
        is BasicBlock.Exit.Ret -> newBB.exit = BasicBlock.Exit.Ret(exit.ret.substitute(rules))
        is BasicBlock.Exit.Unconditional -> newBB.exit =
            BasicBlock.Exit.Unconditional(inlineDefinitions(bb.exit.next!!, rules, visitedBlocks))

        is BasicBlock.Exit.Conditional -> {
            val newCond = exit.cond.substitute(rules)
            newBB.exit = BasicBlock.Exit.Conditional(
                newCond,
                inlineDefinitions(exit.trueBlock, rules, visitedBlocks),
                inlineDefinitions(exit.falseBlock, rules, visitedBlocks)
            )
        }
    }
    return newBB
}

private fun collectReplacements(
    bb: BasicBlock,
    rules: HashMap<Definition.Stamp, Expr> = hashMapOf(),
    visited: HashSet<BasicBlock> = hashSetOf()
) {
    if (bb in visited) return
    visited += bb

    for (asg in bb.children) {
        when {
            asg.rhs !is PhiExpr && bb.allUsages(asg.lhs) == 1 && !bb.isUsedInPhi(asg.lhs) -> {
                rules[asg.lhs] = asg.rhs.substitute(rules)
            }

            asg.rhs is ConstExpr<*> -> rules[asg.lhs] = asg.rhs

            else -> Unit
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

private fun BasicBlock.usagesNotPhi(of: Definition.Stamp): List<Int> {
    return usages(of).filter { this.children[it].rhs !is PhiExpr }
}


private fun BasicBlock.allUsages(stamp: Definition.Stamp, visited: HashSet<BasicBlock> = hashSetOf()): Int {
    if (this in visited) return 0
    visited += this
    var visits = usages(stamp).size

    visits += when (val exit = exit) {
        is BasicBlock.Exit.NoNext -> 0
        is BasicBlock.Exit.Ret -> if (stamp in exit.ret) 1 else 0
        is BasicBlock.Exit.Unconditional ->  exit.next.allUsages(stamp, visited)
        is BasicBlock.Exit.Conditional -> (if (stamp in exit.cond) 1 else 0) + exit.trueBlock.allUsages(stamp, visited) + exit.falseBlock.allUsages(stamp, visited)
    }
    return visits
}

private fun BasicBlock.isUsedInPhi(stamp: Definition.Stamp, visited: HashSet<BasicBlock> = hashSetOf()): Boolean {
    if (this in visited) return false
    visited += this
    var used = usages(stamp).any { this.children[it].rhs is PhiExpr }

    if (used) {
        return true
    }

    used = when (val exit = exit) {
        is BasicBlock.Exit.NoNext -> false
        is BasicBlock.Exit.Ret -> false
        is BasicBlock.Exit.Unconditional -> exit.next.isUsedInPhi(stamp, visited)
        is BasicBlock.Exit.Conditional -> exit.trueBlock.isUsedInPhi(stamp, visited) || exit.falseBlock.isUsedInPhi(stamp, visited)
    }
    return used
}