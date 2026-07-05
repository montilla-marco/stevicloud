rootProject.name = "stevidigital-platform"

// Gradle projects live under apps/ — decouple project name from directory path
// so Gradle tasks stay :product-catalog:build (not :apps:product-catalog:build)
include("product-catalog")
project(":product-catalog").projectDir = file("apps/product-catalog")

// Episode 2+: pricing, inventory will be added here
// include("pricing")
// project(":pricing").projectDir = file("apps/pricing")
