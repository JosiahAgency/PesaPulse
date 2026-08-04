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
// reuse (fixed 1.84+). AGP/UTP also pull older Netty and AGP pulls jose4j on
// tooling paths; pin those to patched versions. AGP itself also pulls
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
                "io.netty:netty-codec:4.1.122.Final",
                "io.netty:netty-codec-http:4.1.122.Final",
                "io.netty:netty-codec-http2:4.1.122.Final",
                "io.netty:netty-handler:4.1.122.Final",
                "org.bitbucket.b_c:jose4j:0.9.6",
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
                "io.netty:netty-codec:4.1.122.Final",
                "io.netty:netty-codec-http:4.1.122.Final",
                "io.netty:netty-codec-http2:4.1.122.Final",
                "io.netty:netty-handler:4.1.122.Final",
                "org.bitbucket.b_c:jose4j:0.9.6"
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
