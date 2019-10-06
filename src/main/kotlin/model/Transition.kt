package main.kotlin.model

data class Transition(var origin: State?,var destination: State? = null,var type: TransitionType,
                      var originElement: AndroidNode? = null,var screenShot: String? = null,
                      var leavesAppCore: Boolean = false, var inputString:String = "", var valuableTransNumber: Int? = null) {

    override fun toString(): String {
        return "${origin?.id?: "No origin state"} ; ${destination?.id?:"No destination state"} ; $type"
    }
}