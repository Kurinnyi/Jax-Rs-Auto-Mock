# Jax-Rs-Auto-Mock
This project provides a glue code and simple declarative DSL to quickly create 
and run Service Virtualization server that matches jax-rs interface contract.
<br>
For a working example, please refer to the example project https://github.com/Kurinnyi/Jax-Rs-Auto-Mock-Example<br>
### Use cases of the project
Common`JAX-RS` REST service contains so-called `Resource` - classes or interfaces with annotations.
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
This information is, in fact, an API contract of web service.<br>
This project is a framework that uses this information to create a mock server.<br>
It is useful in a number of situations. For example: 
* API is defined, but the actual server is not yet ready
* there is a need to check against some hardly reproducible corner cases
* it is just hard or impossible to start an actual server
* to easily configure specific responses for tests

This framework allows matching some specific requests to the server, 
and dynamically create responses from JSON templates 
or by instantiating required objects with code. <br>
It also has some proxy capabilities. 
So it is possible to override some responses while forwarding others to the original system.
Moreover, forwarded requests/responses can be recorded for further replay as mocks. 

##### The simplest standalone mock server for the interface above consists only of two files:
* Actual definition of mock. It uses kotlin DSL to define the expected behavior of the mock.

 ```kotlin
import ua.kurinnyi.jaxrs.auto.mock.apiv2.Mock
import ua.kurinnyi.jaxrs.auto.mock.jersey.StubServer

object Main {
    @JvmStatic
    fun main(args: Array<String>) = StubServer().onPort(8080).start()
}

//Define class of jax-rs interface you want to mock
class MockExample : Mock<HelloRestResourceInterface>({ mock ->

    //do the mocking
    mock.getHello(eq("Ivan"), notNull()).respond {
        //Response header
        header("Some header", "header value")
        //Response body
        "Hello Ivan"
    }

    mock.getHello(any(), notNull()).respond {
        //Response header
        header("Some header", "header value")
        //Response body
        "Hello Guest"
    }
})
```
* And some build file in your preferred build system, to wire up the application.
<br><br>
That is enough to start the mock directly from your IDE or build executable jar or start it in any other way suitable for kotlin/jvm application.

### API for defining mock cases
To start mocking, extend the <B>Mock</B> class.
It is then auto-discovered at the start of the server.
You should pass the lambda function with mocks definitions to the constructor.
There, the method of the resource should be called, and parameters of the method call should be defined with matcher methods.

These calls are evaluated in the order defined</b> in the file, and the first one matching is used to respond. 
The resource call should follow `.respond` section, where specified how to respond to this call.
It is possible to have the same response section for many calls of the same or even different methods if they provide the same type of response.

```kotlin
class MockExample : Mock<HelloRestResourceInterface>({ mock ->
    mock.getHello(eq("Ivan"), notNull()).respond {
        "Hello Ivan"
    }

    mock.getHello(eq("Guest"), notNull())
    mock.getHello(notNull(), notNull()).respond {
        "Hello Guest"
    }
})
```
#### Matcher methods
When calling Resource method, there is a variety of methods to use as matchers:
 * `eq` - use java equals method to check if matches
 * `notEg` - same as above but with negation, matches null
 * `notNullAndNotEq` - same as above but does not match null
 * `any` - matches anything including null
 * `isNull` - matches only null
 * `notNull` - matches anything except null
 * `match` - takes predicate to verify matching. Cannot be used with null
 * `matchNullable` same as above but can work with null
 * `anyInRecord` matches any including null. It has some special meaning in record/replay cases.
 * `bodyMatch` - intercepts the JSON received by the server and use provided predicate to do the matching
 * `bodyMatchRegex` - intercepts the JSON received by the server and use provided regex to do the matching
 * `bodySame` - intercepts the JSON received by the server and match it against the provided string, ignoring any white 
 spaces on both sides
  
 It is also possible to <b>match the headers of received HTTP request</b>. See the example below.

```kotlin
    mock.getHello(eq("Ivan"), notNull())
        .header("Auth",  eq("12345"))
        .respond {   
            "Hello Ivan"
        }  
```
#### Responding
`respond` method accepts the lambda, which should provide the response and is called every time the corresponding case clause matches. 
You can create an appropriate object with code or use some of the following helpful methods for forming the response:
* `body` - allows generating response object from inline JSON or file
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
    .respond {
        body("""
            {
               "field" : " < 10",
               "otherField" : 9
            }
        """)
    }
```  
Or put this JSON into some file and simply do:
```kotlin
    .respond {
        body("/filename.json")
    }
```  
The returned object is available for some additional processing. 
```kotlin
    .respond {
        val response: Dto = body("/filename.json")
        response.field = "amended"
        response
    }
``` 
Also there is embedded [pebble templates engine](https://pebbletemplates.io/). The template arguments should be specified as pairs.
```kotlin
    .respond {
        body("""
            {
               "field" : " < 10",
               "otherField" : {{ number }}
            }
        """, "number" to 12)
    }
```  
* `header` - allows to specify response headers. 
Do not use it for standard headers like Content-Type or Content-Length.
```kotlin
    .respond {
        header("Some header", "header value")
        "Response body"
    }  
```
* `code` - allows to specify response code. Normally the code will be correctly set by platform mechanisms.
But you can use this method to specify code explicitly.
```kotlin
    .respond {
        code(404)
    }  
```
* `bodyRaw` - writes the response directly into the HTTP response stream bypassing all other mechanisms.
It can be useful when you need to return some JSON, which does not match the schema of the response type.
```kotlin
    .respond {
        code(500)
        bodyRaw("Ooops something went wrong")
    }  
```
* `proxyTo` - allows using the mock server as the proxy. More on it in <b>Proxy</b> part.
```kotlin
    .respond {
        proxyTo("http:somerealservices.com/")
    }  
```
#### Captors
Sometimes you may need to get an incoming request parameter while generating the response.
For this case, wrap the appropriate parameter matcher with a captor. See example below:
```kotlin
    val idCaptor = capture<Long>()
    mock.getHello(eq("Ivan"), idCaptor(notNull())).respond {
        val idValue = idCaptor()
        "Hello Ivan $idValue"
    }
```
Captor can be nullable to be able to catch nulls, like `capture<Long?>()`

### Groups
Mocks can be dynamically switched on/off.
It can be useful in case of testing when you need to change the behavior of the system.  
Wrap mocks into groups, to achieve this:
```kotlin
class MockExample : Mock<HelloRestResourceInterface>({ mock ->
    
    group("group1", activeByDefault = false) {
        mock.getHello(eq("Ivan"), notNull()).respond {
            "Hello Ivan from group1"
        }
        
        mock.getHello(notNull(), notNull()).respond {
            "Some other mock in group"
        }
    }

    group("group2") {
        mock.getHello(eq("Ivan"), notNull()).respond {
            "Hello Ivan from group2"
        }
    }
})
```
If not specified explicitly, the group is turned on by default.<br>
Group can wrap any number of mocks. 
The same group name can be used any number of times in same or different mock files.<br>
To turn on some groups, send PUT request with the list like bellow to endpoint `${your_mock_server_url}/group`.
```json
[
    {
        "name": "group1",
        "status": "ACTIVE"
    }
]
```
To turn off, do the same, but with `status` `NON_ACTIVE`.
#### Groups callbacks
It is possible to do some side-effects during group switching.
Bellow, the counter is set to zero every time group1 is enabled.
```kotlin
    var counter = 0
    
    onGroupEnabled("group1") {
        counter = 0
    }
    
    group("group1") {
        mock.getHello(eq("Ivan"), notNull()).respond {
            counter++;
            "$counter"
        }
    }
```
Method `onGroupDisabled` is also available.
### Proxy
This framework can be used to selectively proxy requests to the external system:
```kotlin
        mock.getHello(eq("Ivan"), notNull()).respond {
            proxyTo("http://some-external.url")
        }
```
Alternatively, you can specify to proxy all requests, that do not math any mock.
```kotlin
class MockExample : Mock<HelloRestResourceInterface>({ mock ->

    bypassAnyNotMatched("http://some-external.url")
    
    ...
```
### Record/Replay

With a proxy, it is possible to record incoming requests and their responses as mocks.
So they can be replayed when the same request happens next time.
```kotlin
        mock.getHello(eq("Ivan"), notNull()).respond {
            record()
            proxyTo("http:some-external.url")
        }
```
or record all that do not math any mock
```kotlin
class MockExample : Mock<HelloRestResourceInterface>({ mock ->

    bypassAnyNotMatched("http:some-external.url")
    recordAnyBypassed()
    
    ...
```
By default, these records are serialized into YAML format and printed into the console.
To be able to use them, you have to store them by providing a custom instance of
`ua.kurinnyi.jaxrs.auto.mock.extensions.RecordSaver`.
The records should be placed in `resources/mocks/` folder to be read by the system.

### Server configuration
All the configuration of the server is done via the `StubServer` class.
You can configure things like:
* TCP port of the server
* context paths of the resources
* register JAX-RS providers to be used by underlying Jersey server
* resources to ignore
* groups to enable on start
* how to store and read records
* how to read and deserialize response bodies
* override proxy/record configuration
* and much more

Please explore methods of the `StubServer` and read java docks for more details.

Some of the more dynamic configurations are done by the set of "extension" interfaces.
Please explore the list of them [here](https://github.com/Kurinnyi/Jax-Rs-Auto-Mock/tree/refactoring/src/main/kotlin/ua/kurinnyi/jaxrs/auto/mock/extensions)
