package edu.sokolov.lab1.ssa

fun merge(defs1: List<Definition.Stamp>, defs2: List<Definition.Stamp>): List<PhiExpr> {
    assert(defs1.map { it.definition.name }.toSet().size == defs1.size)
    assert(defs2.map { it.definition.name }.toSet().size == defs2.size)

    val res = arrayListOf<PhiExpr>()

    for (def in defs1) {
        res += PhiExpr(def)
    }

    outdef@ for (def in defs2) {
        for (phi in res) {
            if (def.definition.name in phi.predecessors.map { it.definition.name }) {
                phi += def
                continue@outdef
            }
        }
        res += PhiExpr(def)
    }
    return res
}