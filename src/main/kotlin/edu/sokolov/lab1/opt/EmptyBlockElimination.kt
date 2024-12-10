package edu.sokolov.lab1.opt

import edu.sokolov.lab1.ssa.BasicBlock

fun eliminateEmptyBlocks(
    bb: BasicBlock
) : BasicBlock {
    var bb = bb
    while (true) {
        val newBB = eliminateEmptyBlocks(bb, hashMapOf())
        if (newBB == bb) break
        bb = newBB
    }

    return bb
}

private fun eliminateEmptyBlocks(
    bb: BasicBlock,
    visitedBlocks: HashMap<BasicBlock, BasicBlock> = hashMapOf(),
    eliminateExit: Boolean = false
): BasicBlock {
    if (bb in visitedBlocks) return visitedBlocks[bb]!!

    val newBB = BasicBlock(name = bb.name)
    bb.predecessors.forEach { pred -> newBB.addPredecessor(pred) }
    bb.children.forEach { newBB += it.copy(rhs = it.rhs.substitute(hashMapOf())) }
    visitedBlocks[bb] = newBB

    if (bb.exit is BasicBlock.Exit.Unconditional && bb.exit.next!!.children.isEmpty()) {
        if (eliminateExit || bb.exit.next!!.exit !is BasicBlock.Exit.NoNext) {
            newBB.exit = bb.exit.next!!.exit
            bb.exit.next!!.addPredecessor(newBB)

            return newBB
        }
    } /*else if (bb.exit is BasicBlock.Exit.Conditional) {
        val exit = bb.exit as BasicBlock.Exit.Conditional
        if (exit.trueBlock.children.isEmpty() && (eliminateExit || exit.trueBlock.exit !is BasicBlock.Exit.NoNext)) {
            newBB.exit = exit.copy(trueBlock = exit.trueBlock.exit.next!!)
            exit.trueBlock.exit.next!!.addPredecessor(newBB)
            return newBB
        } else if (exit.falseBlock.children.isEmpty() && (eliminateExit || exit.falseBlock.exit !is BasicBlock.Exit.NoNext)) {
            newBB.exit = exit.copy(falseBlock = exit.falseBlock.exit.next!!)
            exit.falseBlock.exit.next!!.addPredecessor(newBB)
            return newBB
        }
    }*/

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
                eliminateEmptyBlocks(exit.trueBlock, visitedBlocks, eliminateExit),
                eliminateEmptyBlocks(exit.falseBlock, visitedBlocks, eliminateExit)
            )
        }
    }
    return newBB
}