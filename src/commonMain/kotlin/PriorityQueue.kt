class PriorityQueue<T>(val comparator: Comparator<T>) {
    val list = mutableListOf<T>()

    fun isEmpty() = list.isEmpty()
    fun isNotEmpty() = list.isNotEmpty()
    fun remove(e: T) = list.remove(e)
    fun peek() = list.firstOrNull()
    fun poll() = list.removeFirst()
    operator fun contains(e: T) = list.contains(e)

    fun add(e: T) {
        val index = list.binarySearch(e, comparator)
        val insertionPoint = if (index >= 0) index else -index - 1
        list.add(insertionPoint, e)
    }
    fun addAll(es: Iterable<T>) = es.forEach { add(it) }
}