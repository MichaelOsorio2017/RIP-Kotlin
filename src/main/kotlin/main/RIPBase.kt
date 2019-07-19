package main.kotlin.main
import helper.EmulatorHelper.*
import main.kotlin.model.State
import main.kotlin.model.Transition
import org.bouncycastle.util.Strings
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File
import java.io.FileReader
import java.io.FileWriter
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
import org.apache.xmlgraphics.ps.ImageEncodingHelper
import org.mozilla.javascript.tools.shell.JSConsole
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import  main.kotlin.model.TransitionType.*

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

    constructor(configFilePath: String): this(){
        printRIPInitialMessage()
        this.configFilePath = configFilePath
        params = readConfigFile()
        startTime = System.currentTimeMillis()

        isRunning = true
        statesTable = Hashtable()
        states = ArrayList()
        transitions = ArrayList()
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
        val initialTransition = Transition(null, type = TransitionType.FIRST_INTERACTION)
        val initialState = State(hybrid = hybridApp, contextualChanges =  contextualExploration)
        initialState.id = sequentialNumber

        explore(initialState, initialTransition)
        //TODO Implementar Build Files
        //buildFiles()
        println("EXPLORATION FINISHED, ${statesTable?.size?:"NO states"} states discovered, $executedIterations events executed, in $elapsedTime minutes")
        jsConsoleReader?.kill()
    }

    private fun compareScreenShotWithExisting(screenShot: String):State?{
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

    fun enterInput(node: AndroidNode): Int{
        var type = 0
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
        var origin: AndroidNode?
        return when(transition.type){
           GUI_CLICK_BUTTON ->{
               origin = transition.originElement
               tap("${origin?.getCentral1X()}","${origin?.getCentral1Y()}")
               GUI_CLICK_BUTTON
           }
            SCROLL -> {
                origin = transition.originElement
                scroll(origin!!,true)
                SCROLL
            }
            SWIPE -> {
                origin = transition.originElement
                scroll(origin!!, false)
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
                tap("${origin?.getCentral1X()}","${origin?.getCentral1Y()}")
                val type = enterInput(origin!!)
                if(type ==1) GUI_INPUT_TEXT else GUI_INPUT_NUMBER
            }
            else ->{
                null
            }
        }
    }

    fun explore(previousState : State, executedTransition: Transition){
        println("NEW STATE EXPLORATION STARTED")
        currentState = State(hybrid = hybridApp, contextualChanges = contextualExploration)
        simpleTryCatch {
            ifKeyboardHideKeyboard()
            isEventIdle()
            currentState?.run {
                id = sequentialNumber
                val rawXML = getCurrentViewHierarchy()
                val parsedXML = loadXMLFromString(rawXML)
                val screenShot = takeAndPullScreenshot("$id",folderName)
                this.rawXML = rawXML
                this.parsedXML = parsedXML
                //Conditions for find a new state
                rippingOutsideApp = isRippingOutSideApp(parsedXML)
                val foundState = findStateGraph(this)
                val sameState =  compareScreenShotWithExisting(screenShot)
                if(foundState != null || sameState != null || rippingOutsideApp){
                    println("State Already Exists ${
                        if(foundState !=null){
                            currentState = foundState
                            Helper.deleteFile(screenShot)
                            "Found state in graph"
                        }else if(sameState != null){
                            println("SAME STATE FOUND BY IMAGE COMPARISON")
                            Helper.deleteFile(sameState.screenShot!!)
                            val newScreen = File(screenShot)
                            newScreen.renameTo(File(sameState.screenShot))
                            currentState = sameState
                            "Found state by images"
                        }else{
                          Helper.deleteFile(screenShot)
                            currentState = previousState
                            "Ripping outside the app"
                        }
                    }")
                }else{
                    //New State found
                    val activity = getCurrentFocus()
                    takeAndPullXMLSnapshot("${id}",folderName)
                    println("Current ST: $id")
                    activityName = activity
                    statesTable?.put(rawXML,this)
                    states?.add(this)
                    this.screenShot =  screenShot
                    this.retrieveContext(packageName)
                    getNodeImagesFromState(this)
                }

                if(!rippingOutsideApp){
                    if(hasRemainingTransition()){
                        previousState.addPossibleTransition(executedTransition)
                    }
                    this.addOutboundTransition(executedTransition)
                    previousState.addOutboundTransition(executedTransition)
                    executedTransition.destination = this
                    executedTransition.origin = previousState
                    transitions?.add(executedTransition)
                }
                var stateTransition: Transition? = null
                var stateChanges = false
                while(!stateChanges and validExecution()){
                    stateTransition = popTransition()
                    executeTransition(stateTransition)
                    ifKeyboardHideKeyboard()
                    executedIterations++
                    isEventIdle()
                    stateChanges = stateChanges()
                }
                if(stateChanges and validExecution()){
                    val tranScreenshot = takeTransitionScreenshot(stateTransition!!, transitions?.size!!)
                    stateTransition.screenShot = tranScreenshot
                    executedIterations++
                    explore(this,stateTransition)
                }
            }
        }
    }

    private fun findStateGraph(pState: State): State? = statesTable?.get(pState.rawXML)

    private fun ifKeyboardHideKeyboard(){
        if(isKeyBoardOpen()) goBack()
    }
    private fun isRippingOutSideApp(parsedXML: Document): Boolean{
        val currentPackage = parsedXML.getElementsByTagName("node").item(0).attributes.getNamedItem("package").nodeValue
        if(pacName == ""){
            pacName = currentPackage
        }
        println("pacName $pacName")
        println("packageName $packageName")
        if(currentPackage != packageName){
            println("Ripping outside")
            println("Going back")
            goBack()
            return true
        }
        return false
    }

    private fun loadXMLFromString(xml: String): Document{
        val builder = DocumentBuilderFactory.newInstance().run { newDocumentBuilder() }
        val is_ = InputSource(StringReader(xml))
        return builder.parse(is_)
    }

    private fun readConfigFile(): JSONObject? {

        var obj: JSONObject? = null
        FileReader(configFilePath).use { reader ->
            JSONParser().let {
               obj = it.parse(reader) as JSONObject
                apkLocation = obj?.get("apkPath") as String
                folderName = obj?.get("outputFolder") as String
                hybridApp = obj?.get("isHybrid") as Boolean
                executionMode = obj?.get("executionMode") as String
                with(obj?.get("executionParams") as JSONObject){
                    when(executionMode){
                        "events" -> maxIterations = Math.toIntExact(this.get("events") as Long)
                        "time" -> maxTime = Math.toIntExact(this.get("time") as Long)
                    }
                }
            }
        }
        return obj
    }

    fun preProcess(params2: JSONObject){

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
            if(isSwipe){
                scroll(tx2,ty2,tx,ty2)
            }else{
                scroll(tx2,ty2,tx2,ty)
            }
        }catch (e: Exception){
            println("CANNOT SCROLL")
        }
    }

    private fun stateChanges(): Boolean{
        return getCurrentViewHierarchy().let {
            it != currentState?.rawXML
        }
    }
    private fun validExecution(): Boolean{
        elapsedTime = System.currentTimeMillis().run { ((this-startTime)/60000).toInt()}
        return (elapsedTime<maxTime && (maxIterations-executedIterations)>0)
    }
}