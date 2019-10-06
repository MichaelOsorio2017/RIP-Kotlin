import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij") version "0.4.9"
    java
    kotlin("jvm") version "1.3.41"
}

group = "RIP-Kotlin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")
    compile("commons-io:commons-io:2.6")
    compile("com.fasterxml.jackson.core:jackson-core:2.9.4")
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.4")
    compile("com.googlecode.json-simple:json-simple:1.1.1")
    compile("org.apache.logging.log4j","log4j-api","2.11.1")
    compile("org.apache.logging.log4j","log4j-core","2.11.1")
    compile("org.eclipse.jetty","jetty-server","9.2.3.v20140905")
    compile("org.eclipse.jetty","jetty-servlet","9.2.3.v20140905")
    compile("me.tongfei","progressbar","0.5.5")

}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2019.1.1"
}
configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("""
      Add change notes here.<br>
      <em>most HTML tags may be used</em>""")
}
tasks.withType<KotlinCompile>{
    kotlinOptions.jvmTarget = "1.8"
}

tasks.register<Jar>("baseRip"){
    manifest{
        attributes["Main-Class"] = "main.RIPBase"
    }
    baseName = "RIP"
    this.apply {
       
    }
}

tasks.register<Jar>("riprr"){
    manifest{
        attributes["Main-Class"] = "main.RIPRR"
    }
    baseName = "RIPRR"
//    from(configurations.compile.fileCollection().forEach{
//        zipTree(it)
//    })
}
