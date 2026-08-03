// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    kotlin("jvm") version "2.2.0"
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}

// AGP's bundled lint-gradle / android-test-plugin-result-listener-gradle pull in
// org.bouncycastle:bcprov-jdk18on:1.79, vulnerable to GOST 28147-2015 CTR keystream
// reuse (fixed 1.84+). AGP/UTP also pull io.netty:netty-codec-http2:4.1.110.Final
// on tooling paths; pin to a patched Netty line. AGP itself also pulls
// org.jdom:jdom2:2.0.6,
// vulnerable to XXE-triggered DoS in SAXBuilder (fixed 2.0.6.1). Force both up to a
// patched release on the build-tooling classpath until AGP bumps them upstream.
buildscript {
    configurations.classpath {
        resolutionStrategy {
            force(
                "org.bouncycastle:bcprov-jdk18on:1.84",
                "org.bouncycastle:bcpkix-jdk18on:1.84",
                "org.bouncycastle:bcutil-jdk18on:1.84",
                "io.netty:netty-codec-http2:4.1.118.Final",
                "org.jdom:jdom2:2.0.6.1"
            )
        }
    }
}

allprojects {
    configurations.configureEach {
        resolutionStrategy {
            force(
                "org.bouncycastle:bcprov-jdk18on:1.84",
                "org.bouncycastle:bcpkix-jdk18on:1.84",
                "org.bouncycastle:bcutil-jdk18on:1.84",
                "io.netty:netty-codec-http2:4.1.118.Final"
            )
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
}

kotlin {
    sourceSets["main"].kotlin.srcDir("src/main/java")
    sourceSets["test"].kotlin.srcDir("src/test/java")
}
