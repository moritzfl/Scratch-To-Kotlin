plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "picoboard"

include("picoboard")
include("scratch-playground")
include("programming-exercise-tasks")
include("solutions")
