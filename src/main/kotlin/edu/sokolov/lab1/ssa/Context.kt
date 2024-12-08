package edu.sokolov.lab1.ssa

import edu.sokolov.lab1.stat.Stats

/**
 * @param isStrict - should ban unknown identifiers or add it in globals
 */
class Context(val isStrict: Boolean = false, val stats: HashSet<Stats> = hashSetOf()) {
    private val globals = hashMapOf<String, Definition>()

    private val definitions = arrayListOf(hashMapOf<String, Definition>())
    private var scope = 0

    private val currentLevel: HashMap<String, Definition>
        get() = definitions.last()

    fun push() {
        scope++
        definitions += hashMapOf()
    }

    fun pop() {
        scope--
//        definitions.removeLast()
    }

    fun resolve(id: String, bb: BasicBlock): Definition.Stamp {
        for (assignment in bb.children.asReversed()) {
            if (assignment.lhs.definition.name == id) return assignment.lhs
        }

        if (isStrict)
            throw IllegalStateException("Definition $id not found in basic block")
        else {
            val def = Definition(id)
            globals[id] = def
            return def.new()
        }
    }

    fun merge(id: String, bb: BasicBlock): PhiExpr {
        for (assignment in bb.children.asReversed()) {
            if (assignment.lhs.definition.name == id) return PhiExpr(setOf(assignment.lhs))
        }
        val phi = PhiExpr()
        for (predBB in bb.predecessors) {
            phi += resolve(id, predBB)
        }
        return phi
    }

    fun definitions(bb: BasicBlock, withLast: Boolean = true): List<Definition.Stamp> {
        val known = hashSetOf<String>()
        val result = arrayListOf<Definition.Stamp>()
        var isLast = true
        for (child in bb.children.asReversed()) {
            if (!child.lhs.isTmp && child.lhs.definition.name !in known || isLast && withLast) {
                known += child.lhs.definition.name
                result += child.lhs
            }
            isLast = false
        }
        return result
    }

    fun mergedDefinitions(bb1: BasicBlock, bb2: BasicBlock, withLast: Boolean = true): List<PhiExpr> {
        return merge(definitions(bb1, withLast = withLast), definitions(bb2, withLast = withLast))
    }

    fun mergedDefinitions(bb1: BasicBlock, bb2: BasicBlock, withLast1: Boolean, withLast2: Boolean): List<PhiExpr> {
        return merge(definitions(bb1, withLast = withLast1), definitions(bb2, withLast = withLast2))
    }


    private fun String.def(): Definition? {
        for (defs in definitions.asReversed()) {
            if (this in defs) return defs[this]
        }
        return null
    }

    fun introduce(id: String): Definition.Stamp {
        check(id !in currentLevel)
        val def = Definition(id)
        currentLevel[id] = def
        return def.new()
    }

    fun fresh(id: String): Definition.Stamp {
        val def = id.def() ?: let {
            Definition(id).also { currentLevel[id] = it }
        }
        return def.new()
    }

    fun tmp(): Definition.Stamp {
        return fresh(tmpname)
    }

    val Definition.isTmp: Boolean
        get() = name == tmpname

    val Definition.Stamp.isTmp: Boolean
        get() = definition.isTmp

    companion object {
        private const val tmpname = "__tmp"
    }
}
