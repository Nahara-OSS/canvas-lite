package io.github.naharaoss.canvaslite.ext

fun <T : AutoCloseable> MutableList<AutoCloseable>.consume(resource: T): T {
    add(resource)
    return resource
}

fun List<AutoCloseable>.closeAll() {
    for (a in this.reversed()) a.close()
}