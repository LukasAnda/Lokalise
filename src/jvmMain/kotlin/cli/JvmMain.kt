package cli

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        runTranskribe(args)
    }
}
