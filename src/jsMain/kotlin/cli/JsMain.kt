package cli

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

@ExperimentalJsExport
@JsExport @JsName("transkribe")
fun main(args: Array<String>) {
    GlobalScope.promise {
        runTranskribe(args)
    }
}