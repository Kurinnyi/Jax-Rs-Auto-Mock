package example

import example.contract.DtoRestResourceInterface
import ua.kurinnyi.jaxrs.auto.mock.kotlin.StubDefinitionContext
import ua.kurinnyi.jaxrs.auto.mock.kotlin.StubsDefinition

class MockExampleOtherFile : StubsDefinition {
    override fun getStubs(context: StubDefinitionContext) = context.createStubs {

        forClass(DtoRestResourceInterface::class) {
            whenRequest {
                addDto(bodyJson("""
                    {
                       "field" : "json field",
                       "otherField" : 33
                    }
                """))
            } thenResponse {
                bodyJson("""
                    {
                       "field" : "json field",
                       "otherField" : 34
                    }
                """)
            }

            whenRequest {
                addDto(bodyJson("""
                    {
                       "field" : "json field",
                       "otherField" : 35
                    }
                """))
            } thenResponse {
                bodyJsonJersey("""
                    {
                       "field" : "json field",
                       "otherField" : 36
                    }
                """)
            }

            whenRequest {
                addDto(match { it.otherField > 10 })
            } thenResponse {
                bodyJsonJersey("""
                    {
                       "field" : " > 10"
                    }
                """)
            }

            whenRequest {
                addDto(match { it.otherField < 10 })
            } thenResponse {
                bodyJsonJersey("""
                    {
                       "field" : " < 10"
                    }
                """)
            }

            whenRequest {
                addDto(any())
            } thenResponse {
                bodyJsonJersey("""
                    {
                       "field" : "any other"
                    }
                """)
            }

        }
    }
}
