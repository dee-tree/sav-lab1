package edu.sokolov.lab1.ssa

class Context {
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
        definitions.removeLast()
    }

    fun resolve(id: String, bb: BasicBlock): Definition.Stamp {
        for (assignment in bb.children.asReversed()) {
            if (assignment.lhs.definition.name == id) return assignment.lhs
        }

        throw IllegalStateException("Definition $id not found in basic block")
    }

    fun merge(id: String, bb: BasicBlock): PhiExpr {
        for (assignment in bb.children.asReversed()) {
            if (assignment.lhs.definition.name == id) return PhiExpr(listOf(assignment.lhs))
        }
        val phi = PhiExpr()
        for (predBB in bb.predecessors) {
            phi += resolve(id, predBB)
        }
        return phi
    }

    fun definitions(bb: BasicBlock): List<Definition.Stamp> {
        val known = hashSetOf<String>()
        val result = arrayListOf<Definition.Stamp>()
        for (child in bb.children.asReversed()) {
            if (!child.lhs.isTmp && child.lhs.definition.name !in known) {
                known += child.lhs.definition.name
                result += child.lhs
            }
        }
        return result
    }

    fun mergedDefinitions(bb1: BasicBlock, bb2: BasicBlock): List<PhiExpr> {
        return merge(definitions(bb1), definitions(bb2))
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
