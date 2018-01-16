# Build Soap Web Service using STS and Gradle

## Purpose

We are going to build a soap web service that return the information of a specified movie.

## Create a directory structure and initialize a java application in Gradle

```shell
$ mkdir build-soap-web-service-using-sts-gradle
$ cd build-soap-web-service-using-sts-gradle
$ gradle init --type basic
```

This will create a empty project and set up gradle wrapper. Here is the initial directory structure.

```shell
-rwxr-xr-x+ 1 pyang Domain Users 1172 Jan 16 16:10 build.gradle
drwxr-xr-x+ 1 pyang Domain Users    0 Jan 16 16:10 gradle
-rwxr-xr-x+ 1 pyang Domain Users 5296 Jan 16 16:10 gradlew
-rwxr-xr-x+ 1 pyang Domain Users 2260 Jan 16 16:10 gradlew.bat
-rwxr-xr-x+ 1 pyang Domain Users  614 Jan 16 16:10 settings.gradle
```

## Create the source code directory structure
```shell
$ mkdir -p src/main/java/hello
$ mkdir -p src/main/resources
```

## Create a Gradle build file - gradle.build

``` c
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:1.5.9.RELEASE")
    }
}

apply plugin: 'java'
apply plugin: 'eclipse'
apply plugin: 'idea'
apply plugin: 'org.springframework.boot'

repositories {
    mavenCentral()
}

dependencies {
    compile("org.springframework.boot:spring-boot-starter-web-services")
    testCompile("org.springframework.boot:spring-boot-starter-test")
    compile("wsdl4j:wsdl4j:1.6.1")
}
```

This initial build file contains a few features:
1. The Spring Boot gradle plugin provides many convenient features asscociated with Spring Boot applications.
2. Spring web service and wsdl4j are added as dependencies.

## Create an XML schema to define the domain

The web service domain is defined in an XML schema file (XSD) that Spring web service will export automatically as a WSDL.

Create an XSD file with operations to return a movie’s name, genra, director, country and year:

```shell
src/main/resources/movies.xsd
```

```xml
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:tns="http://pheely.io/get-movie-web-service"
           targetNamespace="http://pheely.io/get-movie-web-service" elementFormDefault="qualified">

    <xs:element name="getMovieRequest">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="name" type="xs:string"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>

    <xs:element name="getMovieResponse">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="movie" type="tns:movie"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>

    <xs:complexType name="movie">
        <xs:sequence>
            <xs:element name="name" type="xs:string"/>
            <xs:element name="genra" type="xs:string"/>
            <xs:element name="director" type="xs:string"/>
            <xs:element name="year" type="xs:int"/>
            <xs:element name="country" type="xs:string"/>
        </xs:sequence>
    </xs:complexType>
</xs:schema>
```

## Generate domain classes based on an XML schema
The next step is to generate Java classes from the XSD file. The right approach is do this automatically during build time using a gradle plugin.

Here is the gradle.build file.

``` shell
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:1.5.9.RELEASE")
    }
}

apply plugin: 'java'
apply plugin: 'eclipse'
apply plugin: 'idea'
apply plugin: 'org.springframework.boot'

repositories {
    mavenCentral()
}

task genJaxb {
    ext.sourcesDir = "${buildDir}/generated-sources/jaxb"
    ext.classesDir = "${buildDir}/classes/jaxb"
    ext.schema = "src/main/resources/movies.xsd"

    outputs.dir classesDir

    doLast() {
        project.ant {
            taskdef name: "xjc", classname: "com.sun.tools.xjc.XJCTask",
                    classpath: configurations.jaxb.asPath
            mkdir(dir: sourcesDir)
            mkdir(dir: classesDir)

            xjc(destdir: sourcesDir, schema: schema) {
                arg(value: "-wsdl")
                produces(dir: sourcesDir, includes: "**/*.java")
            }

            javac(destdir: classesDir, source: 1.6, target: 1.6, debug: true,
                    debugLevel: "lines,vars,source",
                    classpath: configurations.jaxb.asPath) {
                src(path: sourcesDir)
                include(name: "**/*.java")
                include(name: "*.java")
            }

            copy(todir: classesDir) {
                fileset(dir: sourcesDir, erroronmissingdir: false) {
                    exclude(name: "**/*.java")
                }
            }
        }
    }
}

task afterEclipseImport {
	dependsOn "genJaxb"
}

// tag::jaxb[]
configurations {
    jaxb
}

jar {
    baseName = 'gs-producing-web-service'
    version =  '0.1.0'
    from genJaxb.classesDir
}

sourceCompatibility = 1.8
targetCompatibility = 1.8

dependencies {
    compile("org.springframework.boot:spring-boot-starter-web-services")
    testCompile("org.springframework.boot:spring-boot-starter-test")
    compile("wsdl4j:wsdl4j:1.6.1")
    jaxb("org.glassfish.jaxb:jaxb-xjc:2.2.11")
    compile(files(genJaxb.classesDir).builtBy(genJaxb))
}
```
