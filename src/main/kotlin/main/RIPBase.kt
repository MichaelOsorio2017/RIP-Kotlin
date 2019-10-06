package main.kotlin.main
import helper.EmulatorHelper.*
import main.kotlin.model.State
import main.kotlin.model.Transition
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.util.*
import kotlin.collections.ArrayList
import helper.Helper
import helper.compareImage
import helper.getNodeImagesFromState
import helper.takeTransitionScreenshot
import main.kotlin.RipException
import main.kotlin.hybridResources.JSConsoleReader
import main.kotlin.model.AndroidNode
import main.kotlin.model.TransitionType
import me.tongfei.progressbar.ProgressBar
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import  main.kotlin.model.TransitionType.*
import org.json.simple.JSONArray
import java.io.*

open class RIPBase(){
    companion object{
        const val AMOUNT_TRANSITIONS = "amountTransitions"
        const val STATES = "states"
        const val TRANSITIONS = "transitions"
        const val AMOUNT_STATES = "amountStates"
    }
    /*
    * Environment variables
    */
    val aapt = System.getenv("AAPT_LOCATION")
    /*
	 * Execution settings
	 */

    /**
     * Indicates if the application is hybrid
     */
    var hybridApp = false
    /**
     * Contextual exploration strategy in the device
     */
    var contextualExploration = false
    /*
	 * Execution variables
	 */
    /**
     * The application was installed
     */
    var appInstalled = false
    /**
     * Folder where the results will be exported
     */
    var folderName = ""
    /**
     * Location of the APK file
     */
    var apkLocation = ""
    /**
     * Package name of the application
     */
    var packageName = ""

    var sequentialNumber = -1
        get() = ++field

    /**
     * Main activity of the application
     */
    var mainActivity = ""

    var currentState: State? = null

    var statesTable: Hashtable<String, State>? = null

    var states: ArrayList<State>? = null

    var transitions: ArrayList<Transition>? =null
    var executedTransitions: ArrayList<Transition>? = null
    /*
	 * Device information
	 */

    /**
     * Android version
     */
    var version = ""
    /**
     * Device resolution
     */
    var resolution = 0
    /**
     * Device dimensions
     */
    var dimensions = ""
    /**
     * Device sensors
     */
    var sensors = ""
    /**
     * Device services
     */
    var services = ""
    /**
     * Auxiliary writer
     */
    var out: FileWriter? = null
    /**
     * Writing Time
     */
    var waitingTime = 0
    /**
     * Indicates if rip is running
     */
    var isRunning = false

    var pacName = ""
    /**
     * Indicates if the ripping is doing outside the app
     */
    var rippingOutsideApp = false
    /**
     * location path to the configuration file
     */
    var configFilePath = ""

    var params: JSONObject? = null

    var executionMode = ""

    var maxIterations = 1000

    var executedIterations = 0

    var maxTime = 1000

    var elapsedTime = 0

    var startTime = 0L

    var finishTime = 0L

    private fun buildFiles(){
        val resultFile = JSONObject()
        resultFile.put(AMOUNT_STATES,statesTable?.size)
        resultFile.put(AMOUNT_TRANSITIONS, transitions?.size)

        //states
        val resultStates = JSONObject()
        states?.forEach{
            val state = JSONObject()
            state.put("id", it.id)
            state.put("activityName",it.activityName)
            state.put("rawXML",it.rawXML)
            state.put("screenShot", it.screenShot)
            resultStates.put("${it.id}",state)
        }
        resultFile.put(STATES, resultStates)

        //Transitions
        val resultTransitions = JSONObject()
        transitions?.forEachIndexed{i, tempTransition ->
            val transition = JSONObject()
            transition.put("stState", tempTransition.origin?.id)
            transition.put("dsState", tempTransition.destination?.id)
            transition.put("tranType", tempTransition.type.name)
            transition.put("outside",tempTransition.leavesAppCore)
            transition.put("screenshot", tempTransition.screenShot)
            if(tempTransition.type == GUI_INPUT_TEXT){
                transition.put("inputString",tempTransition.inputString)
            }
            val originNode = tempTransition.originElement
            if(originNode != null){
                val androidNode = JSONObject()
                androidNode.put("resourceID", originNode.resourceID)
                androidNode.put("name", originNode.name)
                androidNode.put("text",originNode.text)
                androidNode.put("xpath",originNode.xPath)
                if(tempTransition.type == SCROLL || tempTransition.type == SWIPE){
                    val p1 = originNode.point1
                    val p2 = originNode.point2

                    val tapX = p1!![0]
                    val tapX2 = p2!![0]/3*2
                    val tapY = p1[1]
                    val tapY2 = p2[1]/3*2

                    val tX = tapX.toString()
                    val tX2 = tapX2.toString()
                    val tY = tapY.toString()
                    val tY2 = tapY2.toString()

                    if(tempTransition.type == SCROLL){
                        androidNode.put("action","[$tX2,$tY2][$tX2,$tY]")
                    }else{
                        androidNode.put("action","[$tX2,$tY2][$tX,$tY2]")
                    }
                }
                androidNode.put("bounds", "[${originNode.point1!![0]},${originNode.point1!![1]}][" +
                        "${originNode.point2!![0]},${originNode.point2!![1]}]")
                transition.put("androidNode",androidNode)
            }
            resultTransitions.put("$i",transition)
        }
        resultFile.put(TRANSITIONS, resultTransitions)
        //All transitions
        val allTransitions = JSONObject()
        executedTransitions?.forEachIndexed{ i, tempTransition ->
            val transition = JSONObject()
            transition.put("stState", tempTransition.origin)
            transition.put("tranType", tempTransition.type.name)
            transition.put("outside", tempTransition.leavesAppCore)
            transition.put("valTrans",tempTransition.valuableTransNumber)
            allTransitions.put("$i",transition)
        }
        resultFile.put("allTransitions", allTransitions)
        BufferedWriter(FileWriter("$folderName${File.separator}result.json")).use {
            it.write(resultFile.toJSONString())
        }
        buildTreeJSON()
        buildSequentialJSON()
        buildMetaJSON()
    }
    private fun buildMetaJSON(){
        val graph = JSONObject()
        with(graph){
            put("executionMethod", executionMode)
            put("maxEvents", maxIterations.toString())
            put("execEvents", executedIterations.toString())
            put("maxTime",maxTime.toString())
            put("elapsedTime",elapsedTime.toString())
            put("startingDate",Date(startTime).toString())
            put("finishDate",Date(System.currentTimeMillis()))
            put("apk",packageName)
            put("androidVersion", getAndroidVersion())
            put("deviceResolution", getDeviceResolution())
            put("currentOrientation",if(getCurrentOrientation()==0)"Portrait" else "Lanscape")
            put("deviceDimensions", getScreenDimensions())
            BufferedWriter(FileWriter("$folderName${File.separator}meta.json")).use {
                it.write(toJSONString())
            }
        }
    }
    private fun buildSequentialJSON(){
        val graph = JSONObject()
        val resultStates = JSONArray()
        states?.forEach {
            val state = JSONObject()
            with(state){
                put("name",it.activityName)
                put("image",File(it.screenShot).name)
                put("battery",it.battery)
                put("wifi",it.wifiStatus)
                put("memory", it.memory)
                put("cpu", it.cpu)
                put("airplane", it.airplane)
            }
            resultStates.add(state)
        }
        graph.put("nodes", resultStates)

        val resultTransitions = JSONArray()
        transitions?.forEachIndexed{ i, tempTransition ->
            val transition = JSONObject()
            transition.put("id", i)
            val fileName = if(tempTransition.screenShot == null) tempTransition.destination?.screenShot else tempTransition.screenShot
            transition.put("image",  File(fileName!!).name)
            resultTransitions.add(transition)
        }
        graph.put("links", resultTransitions)
        BufferedWriter(FileWriter("$folderName${File.separator}sequential.json")).use {
            it.write(graph.toJSONString())
        }
    }
    private fun buildTreeJSON(){
        val graph = JSONObject()
        val resultStates = JSONArray()
        states?.forEach {tempState ->
            val state = JSONObject()
            with(state){
                put("id",tempState.id)
                put("name", "(${tempState.id}) ${tempState.activityName}")
                val activityName = tempState.activityName
                put("activityName", if(activityName!!.split("/").size > 1){
                        activityName.split("/")[1]
                    }else{
                        activityName.split("/")[0]
                    })
                put("imageName", File(tempState.screenShot).name)
                put("battery", tempState.battery)
                put("wifi", tempState.wifiStatus)
                put("memory", tempState.memory)
                put("cpu", tempState.cpu)
                put("airplane", tempState.airplane)
                put("model", tempState.domainModel)
                resultStates.add(this)
            }
        }
        with(JSONObject()){
            put("id", states?.size)
            put("name","(${states!!.size-1}) End of execution")
            put("activityName", "End of execution")
            put("imageName", "N/A")
            put("battery", "N/A")
            put("wifi","N/A")
            put("memory", "N/A")
            put("cpu","N/A")
            put("airplane","N/A")
            resultStates.add(this)
        }
        graph.put("nodes", resultStates)

        val resultTransitions = JSONArray()
        transitions?.forEachIndexed{ i,tempTransition ->
            with(JSONObject()){
                put("source",tempTransition.origin!!.id!!-1)
                put("target", tempTransition.destination!!.id!!-1)
                put("id",i)
                put("tranType",tempTransition.type.name)
                val temp = tempTransition
                val fileName = if(temp.screenShot == null) temp.destination!!.screenShot else tempTransition.screenShot
                put("imageName", File(fileName).name)
                resultTransitions.add(this)
            }
        }
        val tempTransition = transitions!!.get(transitions?.size!!-1)
        val finalTrans = Transition(origin = tempTransition.destination, type =  FINISH_EXECUTION)
        val transition = JSONObject()
        with(transition){
            put("source", tempTransition.destination!!.id!!-1)
            put("target", states!!.size)
            put("id",transition.size)
            put("tranType", FINISH_EXECUTION.name)
            put("imageName", takeTransitionScreenshot(finalTrans, transition.size))
            resultTransitions.add(transition)
        }
        graph.put("links",resultTransitions)
        BufferedWriter(FileWriter("$folderName${File.separator}tree.json")).use {
            it.write(graph.toJSONString())
        }
    }

    fun compareScreenShotWithExisting(screenShot: String):State?{
        val existing = File(screenShot)
        for(i in states?.size?.downTo(0)?: 0..0){
            val state = states?.get(i)
            with(compareImage(File(state?.screenShot),existing)){
                println("$this ${state?.id}")
                if( this >= 97.5){
                    println("Same!")
                    return state
                }
            }
        }
        return null
    }

    constructor(configFilePath: String): this(){
        printRIPInitialMessage()
        this.configFilePath = configFilePath
        params = readConfigFile()
        startTime = System.currentTimeMillis()

        isRunning = true
        statesTable = Hashtable()
        states = ArrayList()
        transitions = ArrayList()
        executedTransitions = ArrayList()
        File(folderName).mkdir()

        Helper.getInstance(folderName)
        simpleTryCatch {
            version = getAndroidVersion()
        }

        appInstalled = installAPK(apkLocation)
        if (!appInstalled) {
            throw RipException("APK could not be installed")
        }

        if(aapt == null){
            throw RipException("AAPT_LOCATION was not set")
        }

        simpleTryCatch {
            packageName = getPackageName(aapt, apkLocation)
            mainActivity = getMainActivity(aapt, apkLocation)
            startActivity(packageName, mainActivity)
            val pb = ProgressBar("Waiting for the app", 100)
            pb.start()
            for(i in 5.downTo(0)){
                pb.stepBy(20)
                TimeUnit.SECONDS.sleep(1)
            }
            pb.stop()
        }

        var jsConsoleReader: JSConsoleReader? = null
        if(hybridApp){
            jsConsoleReader = JSConsoleReader(this)
            jsConsoleReader.start()
        }

        preProcess(params!!)
        val initialTransition = Transition(null, type = FIRST_INTERACTION)
        val initialState = State(hybrid = hybridApp, contextualChanges =  contextualExploration)
        initialState.id = sequentialNumber
        explore(initialState, initialTransition)

        buildFiles()
        simpleTryCatch {
            clearData(packageName)
            uninstallAPK(packageName)
        }

        println("EXPLORATION FINISHED, ${statesTable?.size?:"NO states"} states discovered, $executedIterations events executed, in $elapsedTime minutes")
        jsConsoleReader?.kill()
    }

    private fun enterInput(node: AndroidNode): Int{
        var type = 0
        moveToEndInput()
        enterInput(checkInputType().let {
            type = it
            if(it == 1){
                "${('A'..'Z').random()}${('a'..'z').random()}${('a'..'z').random()}"
            }else{
                "${Random().nextInt(100)}"
            }
        })
        goBack()
        return type
    }

    fun executeTransition(transition: Transition): TransitionType?{
        val origin: AndroidNode?
        return when(transition.type){
           GUI_CLICK_BUTTON ->{
               origin = transition.originElement
               tap(origin)
               GUI_CLICK_BUTTON
           }
            SCROLL -> {
                origin = transition.originElement
                scroll(origin!!,false)
                SCROLL
            }
            SWIPE -> {
                origin = transition.originElement
                scroll(origin!!, true)
                return SWIPE
            }
            CONTEXT_INTERNET_OFF ->{
                turnInternet(false)
                CONTEXT_INTERNET_OFF
            }
            CONTEXT_INTERNET_ON ->{
                turnInternet(true)
                CONTEXT_INTERNET_ON
            }
            ROTATE_LANDSCAPE ->{
                rotateLandscape()
                ROTATE_LANDSCAPE
            }
            ROTATE_PORTRAIT ->{
                rotatePortrait()
                ROTATE_PORTRAIT
            }
            CONTEXT_LOCATION_OFF ->{
                turnLocationServices(false)
                CONTEXT_LOCATION_OFF
            }
            CONTEXT_LOCATION_ON ->{
                turnLocationServices(true)
                CONTEXT_LOCATION_ON
            }
            BUTTON_BACK ->{
                goBack()
                BUTTON_BACK
            }
            GUI_INPUT_TEXT ->{
                origin = transition.originElement
                tap(origin)
                val type = enterInput(origin!!)
                if(type ==1) GUI_INPUT_TEXT else GUI_INPUT_NUMBER
            }
            else ->{
                null
            }
        }
    }

    open fun explore(previousState : State, executedTransition: Transition){
        println("NEW STATE EXPLORATION STARTED")
        currentState = State(hybrid = hybridApp, contextualChanges = contextualExploration)
        simpleTryCatch {
            processState(previousState,executedTransition)
            var stateTransition: Transition? = null
            var stateChanges = false
            currentState?.apply{
                while(!stateChanges && validExecution()){
                    stateTransition = popTransition()
                    isEventIdle()
                    stateTransition?.also{
                        executeTransition(it)
                        ifKeyboardHideKeyboard()
                        executedIterations++
                        stateChanges = stateChanges()
                        it.valuableTransNumber = transitions?.size?.minus(1)
                        executedTransitions?.add(it)
                        if(isHome()){
                            throw RipException("Execution closed the app: Currently in Home")
                        }
                    }
                }
                if(stateChanges && validExecution()){
                    executedTransitions?.also{ it.removeAt(it.size.minus(1)) }
                }
                if(validExecution()){
                    isEventIdle()
                    stateTransition?.also{
                        it.screenShot = takeTransitionScreenshot(it,transitions?.size?:-1)
                        explore(this,it)
                    }
                }
            }
        }
    }

    fun findStateGraph(pState: State): State? = statesTable?.get(pState.rawXML)

    fun ifKeyboardHideKeyboard(){
        simpleTryCatch {if(isKeyBoardOpen()) goBack()}
    }
    fun isRippingOutSideApp(parsedXML: Document): Boolean{
        val currentPackage = parsedXML.getElementsByTagName("node").item(0).attributes.getNamedItem("package").nodeValue
        if(currentPackage.equals(packageName)||currentPackage.equals("com.google.android.packageinstaller")){
            return false
        }
        println("current package: $currentPackage")
        println("packageName: $packageName")
        println("Ripping outside")
        println("Going back")
        goBack()
        return true
    }

    fun loadXMLFromString(xml: String): Document{
        val builder = DocumentBuilderFactory.newInstance().run { newDocumentBuilder() }
        val is_ = InputSource(StringReader(xml))
        return builder.parse(is_)
    }

    private fun readConfigFile(): JSONObject? {

        var obj: JSONObject? = null
        FileReader(configFilePath).use { reader ->
            JSONParser().also{
               obj = it.parse(reader) as JSONObject
                apkLocation = obj?.get("apkPath") as String
                folderName = obj?.get("outputFolder") as String
                hybridApp = obj?.get("isHybrid") as Boolean
                executionMode = obj?.get("executionMode") as String
                with(obj?.get("executionParams") as JSONObject){
                    when(executionMode){
                        "events" -> maxIterations = Math.toIntExact(get("events") as Long)
                        "time" -> maxTime = Math.toIntExact(get("time") as Long)
                    }
                }
            }
        }
        return obj
    }

    fun processState(previousState: State, executedTransition: Transition){
        ifKeyboardHideKeyboard()
        isEventIdle()
        currentState?.apply{
            id = sequentialNumber
            var rawXML = getCurrentViewHierarchy()
            rawXML = processXML(rawXML)
            val parsedXML = loadXMLFromString(rawXML)
            val screenShot = takeAndPullScreenshot("${currentState?.id}", folderName)
            this.rawXML = rawXML
            this.parsedXML = parsedXML

            //Conditions to find a new state
            rippingOutsideApp = isRippingOutSideApp(parsedXML)
            executedTransition.leavesAppCore = rippingOutsideApp
            executedTransition.valuableTransNumber = transitions?.size?.minus(1) ?: -10
            executedTransitions?.add(executedTransition)
            var sameState: State? = null
            var foundState: State?
            if (rippingOutsideApp) {
                //Application is ripping outside the app
                Helper.deleteFile(screenShot)
                currentState = previousState
                println("Ripping outside the app")
                sequentialNumber--
            } else if (findStateGraph(this).also { foundState = it } != null) {
                //The state is already in the states' graph
                currentState = foundState
                Helper.deleteFile(screenShot)
                println("State Already Exists: Found state in graph")
                sequentialNumber--
            } else if (compareScreenShotWithExisting(screenShot).also { sameState = it } != null) {
                //State is already explored and it was found by image comparison
                sameState?.also {
                    Helper.deleteFile(it.screenShot!!)
                    val newScreen = File(screenShot)
                    newScreen.renameTo(File(it.screenShot))
                    //Change the XML of the state because it could have some change and because of that it was not found in the graph
                    it.rawXML = rawXML
                    it.parsedXML = parsedXML

                    val allNodes = it.parsedXML?.getElementsByTagName("node")
                    val androidNodes = it.stateNodes
                    androidNodes.forEach { androidNode ->
                        for (i in 0 until allNodes!!.length) {
                            val currentNode = allNodes.item(i)
                            val auxAndroidNode = AndroidNode(it, currentNode)
                            if (androidNode.resourceID == auxAndroidNode.resourceID
                                && androidNode.xPath == auxAndroidNode.xPath
                            ) {
                                androidNode.loadAttributesFromDom(currentNode)
                            }
                        }
                    }
                    currentState = it
                }
                println("State Already Exists: Found state by images")
                sequentialNumber--
            } else if (isHome()) {
                throw  RipException("Execution closed the app: Currently in Home")
            } else {
                //New State
                println("New state found")
                isEventIdle()
                this.generatePossibleTransitions()
                val activity = getCurrentFocus()
                takeAndPullScreenshot("$id", folderName)
                println("Current ST: $id")
                activityName = activity
                statesTable?.put(rawXML, this)
                states?.add(this)
                this.screenShot = screenShot
                retrieveContext(packageName)
                getNodeImagesFromState(this)
            }

            //Add out and in bound transitions to the previous state and the current one respectively
            if(!rippingOutsideApp){
                if(this.hasRemainingTransition()){
                    previousState.addPossibleTransition(executedTransition)
                }
                executedTransition.destination = this
                executedTransition.origin = previousState
                this.addInboundTransition(executedTransition)
                previousState.addOutboundTransition(executedTransition)
                transitions?.add(executedTransition)
            }
        }
    }

    open fun processXML(rawXML:String): String = rawXML

    open fun preProcess(params2: JSONObject){

    }
    private fun printRIPInitialMessage(){
        println("\n 2018, Universidad de los Andes\n The Software Design Lab\n")
        println("https://thesoftwaredesignlab.github.io/\n")
        val s = arrayOf(
            "🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥", "🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥",
            "🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥", "🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥 ",
            "🔥🔥   🔥🔥    🔥🔥  🔥🔥          ", "🔥🔥    🔥🔥   🔥🔥  🔥🔥          ",
            "🔥🔥     🔥🔥  🔥🔥  🔥🔥          ", " "
        ).joinToString("\n")
        println(s)
    }
    private fun scroll(origin: AndroidNode, isSwipe: Boolean){
        val p1 = origin.point1
        val p2 = origin.point2

        val tapX = p1!![0]
        val tapX2 = p2!![0]/3*2

        val tapY = p1[1]
        val tapY2 = p2[1]/3*2

        val tx = tapX.toString()
        val tx2 =tapX2.toString()
        val ty = tapY.toString()
        val ty2 = tapY2.toString()

        try{
            if(!isSwipe) scroll(tx2,ty2,tx,ty2) else scroll(tx2,ty2,tx2,ty)
        }catch (e: Exception){
            println("CANNOT SCROLL")
        }
    }

    private fun stateChanges(): Boolean = getCurrentViewHierarchy().run{ this != currentState?.rawXML }

    private fun tap(node: AndroidNode?) = tap("${node?.getCentral1X()}","${node?.getCentral1Y()}")
    private fun validExecution(): Boolean{
        elapsedTime = System.currentTimeMillis().let { ((it-startTime)/60000).toInt()}
        return (elapsedTime<maxTime && (maxIterations-executedIterations)>0)
    }

}