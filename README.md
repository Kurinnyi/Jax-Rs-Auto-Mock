# Jax-Rs-Auto-Mock
This project provides a glue code and simple declarative DSL to quickly create and run mock server that matches your jax-rs interface contract. 
<br>
For more information, please refer to the example project https://github.com/Kurinnyi/Jax-Rs-Auto-Mock-Example<br>
###Use cases of the project
Common`JAX-RS` REST service contains so called `Resource` - classes or interfaces with annotations.
```java
@Path("/helloworld")
public interface HelloRestResourceInterface {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public String getHello(@QueryParam("hi") String hello, @PathParam("id") long id);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("echo/{id}")
    public long echo(@PathParam("id") long id);
}
```
This information is in fact an API contract of web service.<br>
This project is a library that uses this information to create mock server.<br>
It is useful in a number of situation. For example: 
* API is defined but actual server is not yet ready
* there is a need to check against some hardly reproducible corner cases
* it is just hard or impossible to start an actual server
* to easily configure specific responses for tests
* only part of responses are needed to be overridden and other should be proxied to original service
* ...

#####The simplest standalone mock server for the interface above consists only of two files:
* Actual definition of mock. It uses kotlin DSL to define expected behaviour of the mock. And it is inspired by some popular mock frameworks for unit tests.
```kotlin
import example.removeit.HelloRestResourceInterface
import ua.kurinnyi.jaxrs.auto.mock.StubServer
import ua.kurinnyi.jaxrs.auto.mock.kotlin.StubDefinitionContext
import ua.kurinnyi.jaxrs.auto.mock.kotlin.StubsDefinition

object Main {
    @JvmStatic
    fun main(args: Array<String>) = StubServer().setPort(8080).start()
}

class MockExample : StubsDefinition {
    //copy paste this line
    override fun getStubs(context: StubDefinitionContext) = context.createStubs {

        //Define class of jax-rs interface you want to mock
        forClass(HelloRestResourceInterface::class) {
            //do the mocking
            case {
                getHello(eq("Ivan"), anyLong())
            } then {
                //Response header
                header("Some header", "header value")
                //Response body
                "Hello Ivan"
            }            
            
            case {
                getHello(any(), anyLong())
            } then {
                //Response header
                header("Some header", "header value")
                //Response body
                "Hello Guest"
            }
        }
    }
}
```
* And some build file in your preferred build system, to wire up the application.
<br><br>
That is enough to start the mock directly from your IDE or build executable jar or start it in any other way suitable for kotlin/jvm application.

###API for defining mock cases
<B>StubsDefinition</B> interface is the one needed to be implemented.
It is then auto-discovered on start of the server.
It has two methods `getStubs` and `getGroupsCallbacks`, and both of them are optional for everride, but it is expected to override at least one. 
See more information on `getGroupsCallbacks` in section <b>Groups</b>

##### getSubs
The best way to override this method is by calling `createStubs` method on provided `StubDefinitionContext`. `createStubs` accepts lambda function with attached context as `this`, so there is more method to use. 
Inside it, the method `forClass` is available. It is needed to create another context which can benefit from class information. This method receives another lambda function. It can be called any number of times and with different classes. 
 
```kotlin
import example.removeit.HelloRestResourceInterface

class MockExample : StubsDefinition {
    //copy paste this line
    override fun getStubs(context: StubDefinitionContext) = context.createStubs {
        forClass(HelloRestResourceInterface::class) {

        }
        
        forClass(WhateverOtherResourceInterface::class) {

        }
    }
}
```

Inside the lambda passed to `forClass` method `case` is available. It is used to actually define some single mock request
 response matching. Inside it, the instance of the Resource is bind as `this`. So the method of resource should be called and parameters of the method call should be defined with matcher methods.
<br>`case` method goes in pair with `then` method, which is used to provide the response whenever the case clause matches.
The <b>case clauses are evaluated in the order defined</b> in the file, and first one matching is used to provide the response. 
It is possible to have many calls to Resource method in one `case` or even different methods of same Resource if they provide same type of response. In this case it works as <b>or</b>.
```kotlin
    forClass(HelloRestResourceInterface::class) {
        //do the mocking
        case {
            getHello(eq("Ivan"), anyLong())
        } then {
            "Hello Ivan"
        }            
        
        case {
            getHello(eq("Guest"), anyLong())
            getHello(notNull(), anyLong())
        } then {
            "Hello Guest"
        }
    }
```
####Matcher methods
When calling Resource method inside `case` there is a variety of methods to use as matchers:
 * `eq` - use java equals method to check if matches
 * `notEg` - same as above but with negation
 * `any` - matches anything including null
 * `anyBoolean`, `anyInt`, `anyDouble`, `anyLong` - same as above, but to be used with java primitive types
 * `isNull` - matches only null
 * `notNull` - matches anything except null
 * `match` - takes predicate which excepts passed to method data to verify matching. Cannot be used with null
 * `matchNullable` same as above but can work with null
 * `matchBoolean`, `matchInt`, `matchDouble`, `matchLong` - same as above, but for java primitive types
 * `bodyMatch` - intercepts the json received by server and use provided predicate to do matching
 * `bodyMatchRegex` - intercepts the json received by server and use provided regex to do the matching
 * `bodySameJson` - intercepts the json received by server and match it against the provided string, ignoring any white 
 spaces on both sides
  
 It is also possible to <b>match the headers of received http request</b>. See the example bellow.
```kotlin
    case {
        getHello(eq("Ivan"), anyLong())
    }  with {
        header("Auth",  eq("12345"))
    } then {
        "Hello Ivan"
    }  
```
####Providing the response
`then` method accepts the lambda which should provide the response and is called every time the corresponding case 
clause matches. The lambda passed to method receives array of method arguments that was received by resource.
<br>There are also similar methods `then1` to `then5` which receives same arguments but can be typed.
<br> In example bellow the server will response with same number as it receives
```kotlin
    case {
        echo(anyLong())
    } then1 { id:Long ->
        id
    }
```
When forming the response there are some other methods that can be helpful:
* `bodyJson` - allows to generate response object from inline JSON or json file
<br> Suppose the response type is next java class
```java
    public class Dto {
        private String field;
        private Integer otherField;
        //... getters setters whatever 
    
    }
```
Then response can be generated next way:
```kotlin
    then {
        bodyJson("""
            {
               "field" : " < 10",
               "otherField" : 9
            }
        """)
    }
```  
Or put this JSON into some file and simply do:
```kotlin
    then {
        bodyJson("/filename.json")
    }
```  
The returned object is available for some additional processing 
```kotlin
    then {
        val response:Dto = bodyJson("/filename.json")
        response.field = "amended"
        response
    }
``` 
Also there is embedded [pebble templates engine](https://pebbletemplates.io/). The template arguments should be specified as pairs
```kotlin
    then1 { number:Int ->
        bodyJson("""
            {
               "field" : " < 10",
               "otherField" : {{ number }}
            }
        """, "number" to number)
    }
```  
* `header` - allows to specify response headers
```kotlin
    then {
        header("Some header", "header value")
        "Response body"
    }  
```
* `code` - allows to specify response code
```kotlin
    then {
        code(404)
    }  
```
* `bodyRaw` - writes the response directly into http response stream bypassing all other mechanisms.
Can be useful when need to return some JSON which does not match the schema of response type.
```kotlin
    then {
        code(500)
        bodyRaw("Ooops something went wrong")
    }  
```
* `proxyTo` - allows to use the mock server as the proxy. More on it in <b>Proxy</b> part.
```kotlin
    then {
        proxyTo("http:somerealservices.com/")
    }  
```
####Groups
todo
####Proxy
todo
###Server configuration
todo