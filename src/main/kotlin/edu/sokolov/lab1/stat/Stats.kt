package edu.sokolov.lab1.stat

data class Stats(
    val cls: String,

    /**
     * Number of direct children
     */
    val noc: Int,
    /**
     * Coupling between object classes
     */
    val cbo: Int,
    /**
     * Response for a class
     */
    val rfc: Int,
    /**
     * Sum of methods params
     */
    val wcm2: Int,
    /**
     * Average number of arguments per method
     */
    val anam: Double,
    /**
     * Lack of cohesion in methods
     */
    val lcom: Int
) {
}