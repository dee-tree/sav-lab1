package edu.sokolov.lab1.ssa

class Context {
    private val definitions = hashMapOf<String, Definition>()
    fun forName(name: String): Definition.Stamp {
        return definitions.getOrPut(name) { Definition(name) }.new()
    }
}
