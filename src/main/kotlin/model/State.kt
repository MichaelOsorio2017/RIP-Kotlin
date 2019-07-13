package main.kotlin.model

import org.w3c.dom.Document
import java.util.*
import kotlin.collections.ArrayList

data class State(var id: Int? = null, var activityName: String? = null,
                 var rawXML: String? = null, var hybrid: Boolean, var contextualChanges: Boolean,
                 var battery: Int? = null, val stateNodes: List<AndroidNode> = ArrayList(),
                 val possibleTransitions: Deque<Transition> = ArrayDeque(),
                 val outboundTransition: List<Transition> = ArrayList(),
                 val inboundTransitions: List<Transition> = ArrayList(),
                 var screenShot: String? = null, var domainModel: List<Domain> = ArrayList(),
                 var wifiStatus: Boolean? = null, var cpu: Double? = null, var memory:Double? = null,
                 var airplane: Boolean? = null) {
    var parsedXML: Document? = null
        set(value){
            field = value
            generatePossibleTransitions()
        }

    fun popTransition(): Transition = possibleTransitions.pop()


    fun addInboundTransition(pTransition: Transition) = inboundTransitions + pTransition


    fun addOutboundTransition(pTransition: Transition)= outboundTransition + pTransition


    private fun generatePossibleTransitions(){
        possibleTransitions + Transition(origin = this, type = TransitionType.BUTTON_BACK)
        if(contextualChanges){
            listOf(1,2).forEach{
                possibleTransitions + Transition(this, type = TransitionType.CONTEXT_INTERNET_OFF)
            }
            possibleTransitions + Transition(this, type = TransitionType.CONTEXT_LOCATION_OFF)
            possibleTransitions + Transition(this, type = TransitionType.CONTEXT_LOCATION_ON)
            possibleTransitions + Transition(this, type = TransitionType.ROTATE_LANDSCAPE)
            possibleTransitions + Transition(this, type = TransitionType.ROTATE_PORTRAIT)
        }

        if(hybrid){
            //Interactions in hybrid applications
        }

        val allNodes = parsedXML?.getElementsByTagName("node")

        for (i in 0 .. allNodes!!.length){
            var currentNode = allNodes.item(i)
            var newAndroidNode = AndroidNode(this, currentNode)

        }
    }

}
