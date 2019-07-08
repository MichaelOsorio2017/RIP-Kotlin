package helper.EmulatorHelper
import main.kotlin.RipException
import helper.ExternalProcess2.executeProcess2
import helper.Helper
import java.io.File
import java.lang.Exception

/**
 * method to install an apk in a device
 */
fun installAPK(pathAPK: String): Boolean {
    return try {
        val commands = listOf("adb","install","-r",pathAPK)
        executeProcess2(commands, "INSTALLING APK", "Installation completed", "App could not be installed")
        Helper.logMessage("INSTALL APK", pathAPK, null)
        true
    }catch (e: Exception){
        false
    }
}

/**
 * method to keep the device's screen unlocked
 */
fun keepUnLock(){
    try{
        val commands = listOf("adb","shell", "svc", "power", "stayon", "true")
        executeProcess2(commands, "KEEP UNLOCKED", null, null)
    }catch (e: Exception){
        println(e.message)
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
fun pullFile(_remotePath: String,_localPath: String){
    //The kotlin params are val (not mutable) by default so it is necessary store them in new var variables
    var remotePath = _remotePath
    var localPath = _localPath
    val remotePathEx = "/sdcard/sreen.png"
    val localPathEx = "screenPull.png"

    if(remotePath == "" || localPath == ""){
        remotePath = remotePathEx
        localPath = localPathEx
    }

    val commands = listOf("adb", "pull", remotePath, localPath)
    executeProcess2(commands,"PULL FILE","File saved on PC", "File could not be pulled")
}

fun getDeviceResolution(): Int{
    var ans = 0
    try {
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
    }catch (e: Exception){
        println(e.message)
    }

    return ans
}