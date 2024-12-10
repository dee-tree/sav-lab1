// Nested if
fun hello() {
    val list = listOf(1, 2, 3 + 4, last = 5)
    if (list.isEmpty()) {
        val blist = list + 0
        if (blist != list) println("Different!") else println("Same")

        val size = if (blist != list) list.size else blist.size
        System.out.println(size)
    }
}