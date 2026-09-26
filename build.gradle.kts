import org.gradle.jvm.tasks.Jar
import java.util.Properties

plugins {
    java
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isFork = true
    options.forkOptions.javaHome = file(project.findProperty("jdk25Home")?.toString()
        ?: System.getenv("JDK25_HOME")
        ?: "C:\\Program Files\\Java\\jdk-25.0.4.1")
}

fun loadProperties(): Properties {
    return Properties().apply {
        project.file("src/main/resources/EtherMenu/EtherMenu.properties").inputStream().use {
            load(it)
        }
    }
}

group = "EtherMenu"
version = loadProperties().getProperty("version").replace("'", "")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation("org.ow2.asm:asm:9.9.1")
    implementation("org.ow2.asm:asm-tree:9.9.1")
//    implementation(files("lib/fmod.jar"))
//    implementation(files("lib/zombie.jar"))
//    implementation(files("lib/Kahlua.jar"))
//    implementation(files("lib/org.jar"))
    implementation(files("lib/projectzomboid.jar"))
}

// Configuration that only includes ASM (no game JARs)
val asmOnly: Configuration by configurations.creating {
    isCanBeResolved = true
}
dependencies {
    asmOnly("org.ow2.asm:asm:9.9.1")
    asmOnly("org.ow2.asm:asm-tree:9.9.1")
}

tasks.named<Jar>("jar") {
    destinationDirectory.set(project.file("build"))
    archiveFileName.set("EtherMenu-${version}.jar")

    manifest {
        attributes["Main-Class"] = "EtherMenu.Main"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(configurations.runtimeClasspath.get().map { file ->
        if (file.isDirectory) {
            file
        } else {
            zipTree(file)
        }
    }) {
        // Exclude game classes - the patcher reads them from projectzomboid.jar at runtime
        exclude("zombie/**")
        exclude("fmod/**")
        exclude("se/**")
    }
}

tasks.register<Jar>("jarLite") {
    dependsOn("classes")
    destinationDirectory.set(project.file("build"))
    archiveFileName.set("EtherMenu-${version}-lite.jar")

    manifest {
        attributes["Main-Class"] = "EtherMenu.Main"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Lite overlay resources (replace full versions)
    from("src/lite/resources")

    // Main resources, excluding premium panels and license files
    from(sourceSets.main.get().output.resourcesDir!!) {
        exclude("EtherMenu/lua/EtherMenu.lua")
        exclude("EtherMenu/lua/components/panels/EtherSettingsPanel.lua")
        exclude("EtherMenu/lua/components/panels/EtherItemCreator.lua")
        exclude("EtherMenu/lua/components/panels/EtherExploitPanel.lua")
    }

    // Main compiled classes, excluding LicenseManager
    from(sourceSets.main.get().output.classesDirs) {
        exclude("EtherMenu/Ether/LicenseManager.class")
        exclude("EtherMenu/Ether/LicenseManager$*.class")
    }

    // Runtime dependencies (ASM, Lombok, etc.)
    from(configurations.runtimeClasspath.get().map { file ->
        if (file.isDirectory) file else zipTree(file)
    }) {
        // Exclude game classes - the patcher reads them from projectzomboid.jar at runtime
        exclude("zombie/**")
        exclude("fmod/**")
        exclude("se/**")
    }
}

tasks.register<Jar>("jarLiteUC") {
    dependsOn("classes")
    destinationDirectory.set(project.file("build"))
    archiveFileName.set("EtherMenu-${version}-lite-uc.jar")

    manifest {
        attributes["Main-Class"] = "EtherMenu.Main"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // UC-specific overlay (no external links, no full version references)
    from("src/lite-uc/resources")

    // Lite overlay resources (EtherMenu.lua, EtherSettingsPanel.lua)
    from("src/lite/resources")

    // Main resources, excluding premium panels, license files, and info panel (overridden by UC overlay)
    from(sourceSets.main.get().output.resourcesDir!!) {
        exclude("EtherMenu/lua/EtherMenu.lua")
        exclude("EtherMenu/lua/components/panels/EtherSettingsPanel.lua")
        exclude("EtherMenu/lua/components/panels/EtherItemCreator.lua")
        exclude("EtherMenu/lua/components/panels/EtherExploitPanel.lua")
        exclude("EtherMenu/lua/components/panels/EtherInfoPanel.lua")
    }

    // Main compiled classes, excluding LicenseManager
    from(sourceSets.main.get().output.classesDirs) {
        exclude("EtherMenu/Ether/LicenseManager.class")
        exclude("EtherMenu/Ether/LicenseManager$*.class")
    }

    // Runtime dependencies (ASM only - no game JARs for UC compliance)
    from(asmOnly.map { file ->
        if (file.isDirectory) file else zipTree(file)
    })
}
