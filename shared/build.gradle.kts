plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly("org.json:json:20250517")
    testImplementation("org.json:json:20250517")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
