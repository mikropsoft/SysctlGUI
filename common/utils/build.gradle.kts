plugins {
    id("java-library")
    id("kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
    maybeCreate("main").java.srcDir("src/main/kotlin")
}

dependencies {
    implementation("io.insert-koin:koin-core:3.1.3")
}
