import common.isWindows
import common.properties
import common.rider.rdLibDirectory
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

plugins {
    id("plugin-library")
    kotlin("jvm") version "1.6.10"
    id("com.jetbrains.rdgen") version "2021.3.3"
}


dependencies {
    compileOnly(project(":common"))
}



intellij {
    version.set("RD-2021.3.3")
    plugins.set(listOf("rider-plugins-appender"))
    downloadSources.set(false) //there are no sources for rider
    instrumentCode.set(false) // why not??
}




tasks {

    properties("javaVersion", project).let {
        withType<JavaCompile> {
            dependsOn(named("rdgen"))
            options.release.set(it.toInt())
        }
        withType<KotlinCompile> {
            dependsOn(named("rdgen"))
            kotlinOptions.jvmTarget = it
        }
    }


    val setBuildTool = create("setBuildTool") {
        doLast {
            var toolArgs = ArrayList<String>()
            var stdout = ByteArrayOutputStream()
            if (isWindows()) {
                exec {
                    executable = "${projectDir}\\tools\\vswhere.exe"
                    args = listOf("-latest", "-property", "installationPath", "-products", "*")
                    standardOutput = stdout
                    workingDir = projectDir
                }
                var files =
                    groovy.ant.FileNameFinder().getFileNames("${stdout.toString().trim()}\\MSBuild", "**/MSBuild.exe")
                extra["executable"] = files[0]
                toolArgs.add("/v:minimal")
            } else {
                extra["executable"] = "dotnet"
                toolArgs.add("msbuild")
            }

            toolArgs.add(properties("DotnetPluginId", project) + "/" + properties("DotnetSolution", project))
            toolArgs.add("/p:Configuration=" + properties("BuildConfiguration", project))
            toolArgs.add("/p:HostFullIdentifier=")

            extra["args"] = toolArgs.toTypedArray()
        }

    }


    val compileDotNet = create("compileDotNet") {
        dependsOn(named("setBuildTool"))
        doLast {
            var arguments: MutableList<String> = (setBuildTool.extra.get("args") as Array<String>).toMutableList()
            arguments.add("/t:Restore;Rebuild")
            exec {
                executable = setBuildTool.extra.get("executable").toString()
                args = arguments
                workingDir = projectDir
            }
        }
    }


    //rider contributes the dlls to the main project's sandbox
    val prepareSandboxForRider = create("prepareSandboxForRider", Sync::class.java) {
        dependsOn(compileDotNet)

        val dllOutputFolder = "${projectDir}/Digma.Rider.Plugin/Digma.Rider/bin/Digma.Rider/Debug/"
        val dllFiles = listOf(
            "$dllOutputFolder/Digma.Rider.dll",
            "$dllOutputFolder/Digma.Rider.pdb"
        )


        val sandboxDotnetDir = project.rootProject.layout.buildDirectory.dir("idea-sandbox").get().dir("plugins")
            .dir(properties("pluginName", project)).dir("dotnet")

        sandboxDotnetDir.asFile.mkdirs()

        dllFiles.forEach {
            val file = file(it)
            println("dll: " + file)
//            if (!file.exists()) throw GradleException("file $file does not exist")
            from(file)
        }
        into(sandboxDotnetDir)

        doLast {
            dllFiles.forEach {
                val file = file(it)
                if (!file.exists()) throw GradleException("file $file does not exist")
            }
        }
    }

    prepareSandbox {
        dependsOn(prepareSandboxForRider)
    }


}


//todo: create a clean task to clean what rdgen generates
rdgen {

//    val DotnetPluginId = properties("DotnetPluginId",project)
//    val RiderPluginId = properties("RiderPluginId",project).replace('.','/').toLowerCase()
    val modelDir = File(projectDir, "protocol/src/main/kotlin")
    val csOutput = File(projectDir, "Digma.Rider.Plugin/Digma.Rider/Protocol")
    val ktOutput = File(projectDir, "src/main/kotlin/org/digma/rider/protocol")

    verbose = true
    classpath(rdLibDirectory(project).canonicalPath + "/rider-model.jar")
    sources("${modelDir.canonicalPath}/rider/model")
    hashFolder = buildDir.canonicalPath
    packages = "rider.model"

    generator {
        language = "kotlin"
        transform = "asis"
        root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
        namespace = "org.digma.rider.protocol"
        directory = ktOutput.canonicalPath
    }

    generator {
        language = "csharp"
        transform = "reversed"
        root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
        namespace = "Digma.Rider.Protocol"
        directory = csOutput.canonicalPath
    }
}