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

Generated classes are placed in build/generated-sources/jaxb/ directory.

## Create our movie repository

We are going to create a dummy movie repository implementation with hardcoded data. 

```java
package hello;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import io.pheely.get_movie_web_service.Movie;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class MovieRepository {
	private static final Map<String, Movie> movies = new HashMap<>();

	@PostConstruct
	public void initData() {
		Movie titanic = new Movie();
		titanic.setName("Titanic");
		titanic.setYear(1997);
		titanic.setCountry("USA");
		titanic.setGenra("epic romance-disaster");
		titanic.setDirector("James Cameron");

		movies.put(titanic.getName(), titanic);

		Movie pearlHarbor = new Movie();
		pearlHarbor.setName("Pearl Harbor");
		pearlHarbor.setYear(2001);
		pearlHarbor.setCountry("USA");
		pearlHarbor.setGenra("romantic period war drama");
		pearlHarbor.setDirector("Michael Bay");

		movies.put(pearlHarbor.getName(), pearlHarbor);

		Movie spectre = new Movie();
		spectre.setName("Spectre");
		spectre.setYear(2015);
		spectre.setCountry("USA");
		spectre.setGenra("spy");
		spectre.setDirector("Sam Mendes");

		movies.put(spectre.getName(), spectre);
	}

	public Movie findMovie(String name) {
		Assert.notNull(name, "The movie's name must not be null");
		return movies.get(name);
	}
}
```

@PostConstruct annotates a method that needs to be executed after dependency injection is complete to do initialization.

## Create movie service endpoint

To create a service endpoint, you only need a POJO with a few Spring WS annotations to handle the incoming SOAP requests.

``` java
package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import io.pheely.get_movie_web_service.GetMovieRequest;
import io.pheely.get_movie_web_service.GetMovieResponse;

@Endpoint
public class MovieEndpoint {
	private static final String NAMESPACE_URI = "http://pheely.io/get-movie-web-service";

	private MovieRepository movieRepository;

	@Autowired
	public MovieEndpoint(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getMovieRequest")
	@ResponsePayload
	public GetMovieResponse getMovie(@RequestPayload GetMovieRequest request) {
		GetMovieResponse response = new GetMovieResponse();
		response.setMovie(movieRepository.findMovie(request.getName()));

		return response;
	}
}
```

@Endpoint registers the class with Spring WS as a potential candidate for processing incoming SOAP messages.

@PayloadRoot is then used by Spring WS to pick the handler method based on the message’s namespace and localPart.

@RequestPayload indicates that the incoming message will be mapped to the method’s request parameter.

The @ResponsePayload annotation makes Spring WS map the returned value to the response payload.

## Configure web service beans

Create a new class with Spring WS related beans configuration:
