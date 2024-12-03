package edu.sokolov.lab1.opt

import edu.sokolov.lab1.ssa.BasicBlock
import edu.sokolov.lab1.ssa.CallExpr
import edu.sokolov.lab1.ssa.Context
import edu.sokolov.lab1.ssa.Definition
import edu.sokolov.lab1.ssa.Expr
import edu.sokolov.lab1.ssa.MemberCallExpr

fun tmpPropagation(
    ctx: Context,
    bb: BasicBlock,
    rules: HashMap<Definition.Stamp, Expr> = hashMapOf(),
    visitedBlocks: HashMap<BasicBlock, BasicBlock> = hashMapOf(),
    eliminateDefinitions: Boolean = false
): BasicBlock {
    if (bb in visitedBlocks) return visitedBlocks[bb]!!
    val newBB = BasicBlock(name = bb.name)
    bb.predecessors.forEach { pred -> newBB.addPredecessor(pred) }
    visitedBlocks[bb] = newBB

    for (asg in bb.children) {
        when {
             with(ctx) { asg.lhs.isTmp } && asg.rhs !is CallExpr && asg.rhs !is MemberCallExpr -> {
                 rules[asg.lhs] = asg.rhs.substitute(rules)
                if (!eliminateDefinitions) newBB += asg
            }

            else -> newBB += asg.copy(rhs = asg.rhs.substitute(rules))
        }
    }

    when (bb.exit) {
        is BasicBlock.Exit.NoNext -> newBB.exit = BasicBlock.Exit.NoNext
        is BasicBlock.Exit.Ret -> {
            val exit = bb.exit as BasicBlock.Exit.Ret
            newBB.exit =
                if (exit.ret in rules) BasicBlock.Exit.Ret(exit.ret.substitute(rules)) else exit
        }

        is BasicBlock.Exit.Unconditional -> newBB.exit =
            BasicBlock.Exit.Unconditional(tmpPropagation(ctx, bb.exit.next!!, rules, visitedBlocks))

        is BasicBlock.Exit.Conditional -> {
            val exit = bb.exit as BasicBlock.Exit.Conditional
            val newCond = exit.cond.substitute(rules)
            newBB.exit = BasicBlock.Exit.Conditional(
                newCond,
                tmpPropagation(ctx, exit.trueBlock, rules, visitedBlocks),
                tmpPropagation(ctx, exit.falseBlock, rules, visitedBlocks)
            )
        }
    }
    return newBB
}