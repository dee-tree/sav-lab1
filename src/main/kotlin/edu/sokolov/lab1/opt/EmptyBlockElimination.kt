package edu.sokolov.lab1.opt

import edu.sokolov.lab1.ssa.BasicBlock

fun eliminateEmptyBlocks(
    bb: BasicBlock,
    visitedBlocks: HashMap<BasicBlock, BasicBlock> = hashMapOf(),
    eliminateExit: Boolean = false
): BasicBlock {
    if (bb in visitedBlocks) return visitedBlocks[bb]!!

    val newBB = BasicBlock(name = bb.name)
    bb.children.forEach { newBB += it.copy(rhs = it.rhs.substitute(hashMapOf())) }
    visitedBlocks[bb] = newBB

    if (bb.exit is BasicBlock.Exit.Unconditional && bb.exit.next!!.children.isEmpty()) {
        if (eliminateExit || bb.exit.next!!.exit !is BasicBlock.Exit.NoNext) {
            newBB.exit = bb.exit.next!!.exit
            return eliminateEmptyBlocks(newBB)
        }
    }

    when (val exit = bb.exit) {
        is BasicBlock.Exit.NoNext -> newBB.exit = BasicBlock.Exit.NoNext
        is BasicBlock.Exit.Ret -> newBB.exit = BasicBlock.Exit.Ret(exit.ret.substitute(hashMapOf()))
        is BasicBlock.Exit.Unconditional -> newBB.exit = BasicBlock.Exit.Unconditional(
            eliminateEmptyBlocks(bb.exit.next!!, visitedBlocks)
        )
        is BasicBlock.Exit.Conditional -> {
            val newCond = exit.cond.substitute(hashMapOf())
            newBB.exit = BasicBlock.Exit.Conditional(
                newCond,
                eliminateEmptyBlocks(exit.trueBlock, visitedBlocks),
                eliminateEmptyBlocks(exit.falseBlock, visitedBlocks)
            )
        }
    }
    return newBB
}