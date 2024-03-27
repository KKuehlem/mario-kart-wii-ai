@echo off
:: Run with native tools 
echo "Compiling App"
mvn install

echo "Building Native Image"
F:\SDKs\GraalVM\21.3.0_Enterprise\graalvm-ee-java17-21.3.0\bin\native-image.cmd -jar ../target/MarioKartWiiAi-latest-jar-with-dependencies.jar