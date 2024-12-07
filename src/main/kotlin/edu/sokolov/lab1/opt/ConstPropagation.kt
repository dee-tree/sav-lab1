package edu.sokolov.lab1.opt

import edu.sokolov.lab1.ssa.BasicBlock
import edu.sokolov.lab1.ssa.ConstExpr
import edu.sokolov.lab1.ssa.Definition

fun constPropagation(
    bb: BasicBlock,
    rules: HashMap<Definition.Stamp, ConstExpr<*>> = hashMapOf(),
    visitedBlocks: HashMap<BasicBlock, BasicBlock> = hashMapOf(),
    eliminateDefinitions: Boolean = false
): BasicBlock {
    if (bb in visitedBlocks) return visitedBlocks[bb]!!
    val newBB = BasicBlock(name = bb.name)
    bb.predecessors.forEach { pred -> newBB.addPredecessor(pred) }
    val consts = hashMapOf<Definition.Stamp, ConstExpr<*>>().apply { putAll(rules) }
    visitedBlocks[bb] = newBB

    for (assignment in bb.children) {
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

    when (val exit = bb.exit) {
        is BasicBlock.Exit.NoNext -> newBB.exit = BasicBlock.Exit.NoNext
        is BasicBlock.Exit.Ret -> newBB.exit = BasicBlock.Exit.Ret(exit.ret.substitute(consts))
        is BasicBlock.Exit.Unconditional -> newBB.exit =
            BasicBlock.Exit.Unconditional(constPropagation(bb.exit.next!!, consts, visitedBlocks, eliminateDefinitions = eliminateDefinitions))

        is BasicBlock.Exit.Conditional -> {
            val newCond = exit.cond.substitute(consts)
            newBB.exit = BasicBlock.Exit.Conditional(
                newCond,
                constPropagation(exit.trueBlock, consts, visitedBlocks, eliminateDefinitions = eliminateDefinitions),
                constPropagation(exit.falseBlock, consts, visitedBlocks, eliminateDefinitions = eliminateDefinitions)
            )
        }
    }
    return newBB
}