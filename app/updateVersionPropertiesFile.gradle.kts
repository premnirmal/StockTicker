import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

// Used by github action in version-code.yml
tasks.create("updateVersionPropertiesFile") {
    doLast {
        println("Updating version.properties file for F-droid")

        updateVersionPropertiesFile()
    }
}

fun updateVersionPropertiesFile() {
    val rootDir = projectDir.absolutePath
    val filePath =  "$rootDir/app/version.properties"

    val name = getVersionName()
    val major = name.split(".")[0].toInt()
    val minor = name.split(".")[1].toInt()
    val patch = name.split(".")[2].toInt()
    val code = (major * 100000000) + (minor * 100000) + patch

    val output = buildString {
        append("versionName=$name")
        append("\n")
        append("versionCode=$code")
    }
    val file = File(filePath)
    file.writeText(output.toString())
}


fun getVersionName(): String {
    return ByteArrayOutputStream().use { outputStream ->
        val stdout = outputStream
        exec {
            commandLine("git", "describe", "--tags", "--abbrev=0")
            standardOutput = stdout
        }
        stdout.toString().trim()
    }
}
