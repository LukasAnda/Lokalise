package cli

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

@JsExport @JsName("transkribe")
fun main(args: Array<String>) {
    GlobalScope.promise {
        runTranskribe(args)
    }
}