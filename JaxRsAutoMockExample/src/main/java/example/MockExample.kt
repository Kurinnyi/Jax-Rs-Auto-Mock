package example

import example.contract.Dto
import example.contract.DtoRestResourceInterface
import example.contract.HelloRestResourceInterface
import ua.kurinnyi.jaxrs.auto.mock.kotlin.StubDefinitionContext
import ua.kurinnyi.jaxrs.auto.mock.kotlin.StubsDefinition

class MockExample : StubsDefinition {
    override fun getStubs(context: StubDefinitionContext) = context.createStubs {

        forClass(HelloRestResourceInterface::class) {
            whenRequest {
                getHello(any(), any())
            } with {
                header("Auth", isNull())
            } thenResponse {
                code(401)
            }

            whenRequest {
                getHello(notNull(), eq(12))
                getHello(notNull(), eq(14))
            } thenResponse {
                body("Hello 12 or 14")
            }

            whenRequest {
                getHello(any(), any())
            } thenResponse {
                body("Hello any other")
                header("Some header", "header value")
            }
        }

        forClass(DtoRestResourceInterface::class){
            whenRequest {
                getDto()
            } with {
                header("Auth", eq("123"))
            } thenResponse {
                body(Dto("some field", 42))
            }

            whenRequest {
                getDto()
            } thenResponse {
                bodyJson("""
                    {
                       "field" : "json field",
                       "otherField" : 33
                    }
                """)
            }
        }
    }
}
