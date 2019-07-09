package helper.EmulatorHelper
import com.intellij.diff.comparison.expandWhitespaces
import com.intellij.formatting.blocks.split
import main.kotlin.RipException
import helper.ExternalProcess2.executeProcess2
import helper.Helper
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception

//get the try codes and tries to do the action and throws the exception if something goes wrong
inline fun simpleTryCatch(action: () -> Unit){
    try{
        action()
    }catch (t: Throwable){
        println("Caught something with simpleTryCatch method: ${t.message}")
    }
}
/**
 * method to install an apk in a device
 */
fun installAPK(pathAPK: String): Boolean {
    simpleTryCatch {
        val commands = listOf("adb", "install", "-r", pathAPK)
        executeProcess2(commands, "INSTALLING APK", "Installation completed", "App could not be installed")
        Helper.logMessage("INSTALL APK", pathAPK, null)
        return true
    }
    return false
}

/**
 * method to keep the device's screen unlocked
 */
fun keepUnLock(){
   simpleTryCatch {
       val commands = listOf("adb","shell", "svc", "power", "stayon", "true")
       executeProcess2(commands, "KEEP UNLOCKED", null, null)
   }
}

/**
 * Verify the ADB is currently in the system path
 * @return current ADB version
 */
fun verifyADB(): String{
    val commands = listOf("adb", "version")
    val results = executeProcess2(commands, "CHECK ADB INSTALLATION", "Verification complete",
                    "ADB is not in your path")
    val output = results.first()
    val success = output.split("\n")
    val version =success.first().split(" ")
    return version[4]
}

/**
 * Uninstalls an apk with its package
 *
 * @param packageName
 *            is the name of the package that contains the app
 * @throws Exception
 *             if there is no package with that name
 */
fun unistallAPK(packageName: String){
    val commands =  listOf("adb", "uninstall", packageName)
    executeProcess2(commands, "UNINSTALL APK", "Uninstall complete", "APK could not be uninstalled")
    println("UNINSTALL COMPLETE")
}

/**
 * Starts a remote shell and calls the activity manager (am) to launch a
 * specific activity
 *
 * @param activity
 *            is the path of the activity inside package
 * @throws RipException
 * @throws IOException
 * @throws SecurityException
 *             if permission is denied
 * @throws Exception
 *             if activity does not exist or warning if activity is a current
 *             task
 */
fun startActivity(packageName: String, activity: String){
    val commands = listOf("adb", "shell", "am", "start", "-n", "$packageName/$activity")
    executeProcess2(commands, "START ACTIVITY", "Activity launched", "Activity could not be started")
}

/**
 * Stops all the processes related to the specified package. It does not
 * uninstall
 *
 * @param packageName
 *            is the name of the package that contains the app
 * @throws Exception
 *             if something in the process fails, like app does not exist
 */
fun stopApp(packageName: String){
    val commands = listOf("adb","shell","am", "force-stop",packageName)
    executeProcess2(commands, "STOP APPLICATION","Application stopped", "Application could not be stopped")

}

/**
 * Clears all data related to the specified package due to package manager(pm).
 * This command will stop the app too.
 *
 * @param packageName
 *            is the name of the package that contains the app
 * @throws Exception
 *             if something in the process fails, like app does not exist
 */
fun crearData(packageName: String){
    val commands = listOf("adb", "shell", "pm", "clear", packageName)
    executeProcess2(commands, "CLEAR DATA", "Data cleared", "Data could not be cleared")

}

/**
 * Takes a screenshot of the actual device's screen.
 *
 * @param Filename
 *            is the path where the file will be saved.
 * @throws RipException
 * @throws IOException
 * @throws Exception
 *             if filename is wrong, not valid directory
 */
fun takeAndPullScreenshot(id: String, folderName: String): String{
    val screenCapName = "$id.png"
    val fileName = "sdcard/$screenCapName"
    val local = folderName + File.separator + screenCapName
    val commands = listOf("adb", "shell", "screencap", fileName)
    executeProcess2(commands,"TAKE SCREENSHOT", "Screenshot saved at: " + fileName, "Screenshot was not captured")
    pullFile(fileName,local)
    return  local
}

/**
 * Extracts a file from device or emulator (remote) to computer (local).
 *
 * @param RemoteFilePath
 *            must be replaced with the path of the file in the device
 * @param LocalFilePath
 *            is the path where the file will be stored in the computer.
 * @throws RipException
 * @throws IOException
 * @throws Exception
 *             if any path is wrong
 */
fun pullFile(remotePath: String = "/sdcard/sreen.png",localPath: String = "screenPull.png"){

    val commands = listOf("adb", "pull", remotePath, localPath)
    executeProcess2(commands,"PULL FILE","File saved on PC", "File could not be pulled")
}

/**
 * Calls the window manager (wm) and gets the density/resolution of the screen
 *
 * @return int Device density
 */
fun getDeviceResolution(): Int{
    var ans = 0
    simpleTryCatch {
        val commands = listOf("adb", "shell", "wm", "density")
        val results = executeProcess2(commands,"GET DEVICE RESOLUTION", null, null)
        val output = results.first()
        val result = output.split(" ")
        val temp = result[2].toCharArray()
        var fin = charArrayOf()
        for(i in 0..temp.size){
            fin[i] = temp[i]
        }
        val def = String(fin)
        ans = Integer.parseInt(def)
        println("pulling file ans: $ans")
        return ans
    }
    return ans
}

fun isEventIdle(){
    //The original code creates a new String[]{Here the same list of strings}
    val pBB = ProcessBuilder("adb","shell","dumsys","window","-a","|","grep","mAppTransitionState")
    var termino = false
    var pss: Process
    println("waiting for emulator's event idle state")
    var reader: BufferedReader
    var line: String
    var resp = ""

    while(!termino){
        pss = pBB.start()
        reader = BufferedReader(InputStreamReader(pss.inputStream))
        //por cada linea en el reader las concatena con resp
        reader.forEachLine { resp += it }

        pss.waitFor()

        if(resp.contains("IDLE")){
            termino = true
            Thread.sleep(500)
            println("Emulator now is in event idle state")
        }else{
            Thread.sleep(2000)
        }
    }
}

/**
 * Gets the current orientation of the accelerometer
 *
 * @return 0 if it's current orientation is portrait, 1 if landscape
 */

fun getCurrentOrientation(): Int{
    simpleTryCatch {
        val commands = listOf("adb", "shell","dumpsys", "input", "|", "grep",
            "'SurfaceOrientation'")
        val results = executeProcess2(commands, "GET CURRENT ORIENTATION", null, null)
        var ans = results.first().split(":")

        with(ans[1]) {
            replace("\\s", "")
           return Integer.parseInt(this)
        }
    }
    return 0
}

/**
 * Simulates a tap in the screen.
 *
 * @param CoordX
 *            X coordinate
 * @param Coordy
 *            Y coordinate
 * @throws RipException
 * @throws IOException
 */

fun tap(coordX: String = "168", coordY: String = "680"){
    val commands = listOf("adb", "shell", "input", "tap", coordX, coordY)
    executeProcess2(commands,"TAP", null, null)
}

/**
* Simulates a long tap in the screen.
*
* @param CoordX
*            and CoordY are the coordinates of the touch and milliseconds, the
*            time of it.
*/
fun longTap(coordX: String = "168", coordY: String = "680", ms: String = "1000"){
    val commands = listOf("adb", "shell", "input", "swipe", coordX, coordY, coordX, coordY, ms)
    executeProcess2(commands, "LONG TAP", null, null)
}

/**
 * Simulates a scroll
 *
 * @param coordX1
 *            and coordY1 are the source coordinate
 * @param coordX2
 *            and coordY2 destination coordinate
 * @param Milliseconds,
 *            time of the touch.
 */

fun scroll(coordX1: String = "168", coordY1: String = "680",coordX2: String = "200", coordY2: String = "700", ms: String = "1000"){
    val commands = listOf("adb", "shell", "input", "swipe", coordX1, coordY1, coordX2, coordY2, ms)
    executeProcess2(commands, "SCROLL", null, null)
}

/**
 * Simulates the effect of touching back soft button, return to the last
 * activity. 4 is KEYCODE_BACK
 * @throws RipException
 * @throws IOException
 */

fun goBack(){
    val commands = listOf("adb", "shell", "input", "keyevent", "4")
    executeProcess2(commands, "GO BACK", null, null)
}

/**
 * Simulates the effect of touching recents soft button, shows recent apps. 187
 * is KEYCODE_APP_SWITCH
 */
fun showRecents(){
    simpleTryCatch {
        println(" RECENTS \n [adb shell input keyevent 187] \n")
        val commands = listOf("adb","shell", "input", "keyevent", "187")
        executeProcess2(commands, "RECENT", null, null)
    }
}

/**
 * Simulates the effect of pressing the power button. 26 is KEYCODE_POWER
 */
fun turnOnScreen(){
    val commands = listOf("adb", "shell", "input", "keyevent", "26")
    executeProcess2(commands, "TURN ON/OFF SCREEN", null, null)
}

/**
 * Lists available avds
 *
 * @throws Exception
 *             if cannot list emulators.
 */
fun getEmulators(){
    println(" LISTING EMULATORS \n [emulator -list-avds] ")
    val commands = listOf("~/Library/Android/sdk/tools/emulator", "-list-avds")
    executeProcess2(commands,"LIST EMULATORS", null, null )
}

/**
 * Launch an existing emulator on the list
 *
 * @param emulator
 *            Name of the emulator to launch
 * @throws Exception
 *             if the emulator does not exist
 */
fun launchEmulator(emulator: String = "Nexus_5_API_27"){
    val commands = listOf("~/Library/Android/sdk/tools/emulator", "-avd", emulator, "-netdelay",
        "none", "-netspeed", "full")
    executeProcess2(commands,"LAUNCH EMULATOR", null, null)
}

/**
 * Shows logcat and stops.
 *
 * @throws Exception
 *             if something goes wrong
 */
fun showLogcat(){
    val commands = listOf("adb", "shell", "logcat", "-d")
    executeProcess2(commands,"GET LOGCAT", null, null )
}

/**
 * Cleans logcat.
 *
 * @throws Exception
 *             if something goes wrong
 */
fun clearLogcat(){
    val commands = listOf("adb", "shell", "logcat", "-c")
    executeProcess2(commands,"CLEAR LOGCAT", "LOGCAT Cleared", "Error clearing LOGCAT" )
}

/**
 * Gets logcat and saves it in a file, then, pulled it into pc
 *
 * @param path
 *            place where logcat will be saved inside phone
 * @param localPath
 *            place where logcat will be saved in pc
 * @throws Exception
 *             if file cannot be created or pulled
 */
fun saveLogcat(path: String = "/sdcard/outputLogcat.txt", localPath: String = "out.txt"){
    createFile(path)
    val commands = listOf("adb", "shell", "logcat", "-d", ">", path)
    executeProcess2(commands,"SAVE LOGCAT", null, null )
    pullFile(path,localPath)
}

/**
 * Creates a file to save the logcat later
 *
 * @param path
 *            the file path where you want to create the file
 * @throws Exception
 *             if permission is denied
 */
fun createFile(path: String = "/sdcard/outputLogcat.txt"){
    val commands = listOf("adb", "shell", "touch", path)
    executeProcess2(commands, "CREATE A FILE", "File created at: " + path, "File could not be created")
}
/**
 * Turns off automatic rotation
 *
 * @throws RipException
 * @throws IOException
 */
fun turnOffRotation(){
    val commands = listOf("adb", "shell", "content", "insert", "--uri", "content://settings/system",
        "--bind", "name:s:accelerometer_rotation", "--bind", "value:i:0")
    executeProcess2(commands,"TURN OFF AUTOMATIC ROTATION", null, null)
}

/**
 * Rotates to landscape. It is necessary turn off automatic rotation.
 *
 * @throws RipException
 * @throws IOException
 */
fun rotateLandscape(){
    turnOffRotation()
    val commands = listOf("adb", "shell", "content", "insert", "--uri", "content://settings/system",
        "--bind", "name:s:user_rotation", "--bind", "value:i:1")
    executeProcess2(commands, "ROTATE TO LANDSCAPE", null, null )
}

 /**
 * Rotates to portrait. It is necessary turn off automatic rotation.
 *
 * @throws RipException
 * @throws IOException
 */
fun rotatePortrait(){
     turnOffRotation()
     val commands = listOf("adb", "shell", "content", "insert", "--uri", "content://settings/system",
         "--bind", "name:s:user_rotation", "--bind", "value:i:0")
     executeProcess2(commands,"ROTATE TO PORTRAIT", null, null)
 }
/**
 * Open keyboard, the one that has letters
 */
fun showKeyboard(){
    simpleTryCatch {
        val commands = listOf("adb", "shell", "input", "keyevent", "78" )
        executeProcess2(commands, "SHOW KEYBOARD", null, null)
    }
}

/**
 * Open a keyboard, the one that has numbers
 */
fun showNumKeyboard(){
    val commands = listOf("adb", "shell", "input", "keyevent", "58")
    executeProcess2(commands,  "SHOW NUMERIC KEYBOARD", null, null)
}

/**
 * Writes text in a text field
 *
 * @param input
 *            is the string that the users wants to write
 */
    //TODO guardar los valores ingresados en cada input field para luego usarlos en el replay
fun enterInput(input: String){
    val newString = input.replace(" ", "%s")
    val commands = listOf("adb", "shell", "input", "text", newString)
    executeProcess2(commands,"INPUT TEXT", null, null )
}
/**
 * Takes a XML snapshot of the current activity
 *
 * @param destinationRoute
 *            File destination route. If destinationRoute == null, the file
 *            won't be saved
 * @return XML file in String format
 */
fun takeAndPullXMLSnapshot(id: String, folderName: String ): String{
    val commands = listOf("adb", "shell", "uiautomator", "dump")
    val answer = executeProcess2(commands, "TAKE XML SNAPSHOT", null, null)
    val temp = answer.first().split("\n")
    val route = temp.first().split("UI hierchary dumped to: ").first().replace("(\\r)", "")
    val local = "$folderName${File.separator}$id.xml"
    pullFile(route, local )
    return local
}

fun getCurrentViewHierarchy(): String{
    val commands = listOf("adb", "shell", "uiautomator", "dump")
     val route = with(executeProcess2(commands,"TAKE XML SNAPSHOT", null, null ).first().split("\n").first()){
         split("UI hierchary dumped to: ").first().replace("(\\r)", "")
     }
    val readCommands = listOf("adb", "shell", "cat", route)
    val response = executeProcess2(readCommands, "READ XML SNAPSHOT", null, null)
    return  response.first()
}
/**
 * Reads a remote file and returns its content.
 *
 * @param remoteRoute
 *            Remote file route.
 * @return File's content
 */

fun readRemoteFile(remoteRoute: String): String{
    val commands = listOf("adb", "shell", "cat", remoteRoute)
    return executeProcess2(commands,"READ REMOTE FILE", null, null )
            .first()
}
/**
 * Shows history of CPU usage with the name of the packages and the
 * corresponding percentage
 *
 * @param packageName
 *            is the name of the package that we want to know cpu usage
 * @return usage % of the package
 * @throws Exception
 *             if there is no device or emulator or if the package does not
 *             exist
 */
fun showCPUUsage(packageName: String): Double{
    val commands = listOf("adb", "shell", "dumpsys", "cpuinfo", "|", "grep", packageName)
    val answer = executeProcess2(commands,"SHOW CPU USAGE", null, null )
    val ans =
        if(answer.first().isNotEmpty()){
                    with(answer.first().split("%").first().replace("\\s","")){
                        if(contains("\\+")){
                            replace("\\+", "")
                        }else{this}
                    }.toDouble()
        }else{0.0}
    println("CPU: $ans")
    return ans
}
/**
 * Shows history of memory usage with the name of the packages and the
 * corresponding percentage
 *
 * @param packageName
 *            is the name of the package that we want to know memory usage
 * @return Memory usage in K.
 * @throws Exception
 *             if there is no device or emulator or if the package does not
 *             exist
 */
fun showMemoryUsage(packageName: String = "com.example.lanabeji.dailyexpenses"): Double{
    val commands = listOf("adb", "shell", "dumpsys", "meminfo", "|", "grep", packageName)
    with(executeProcess2(commands, "SHOW MEMORY USAGE", null, null).first().split("\n")){
        if(isNotEmpty()){

        }
    }
}
