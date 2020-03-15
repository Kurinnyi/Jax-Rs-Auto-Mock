package ua.kurinnyi.jaxrs.auto.mock.filters

import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class HttpRequestResponseHolder : Filter {

    private val requestHolder = ThreadLocal<BufferingRequestWrapper>()
    private val responseHolder = ThreadLocal<HttpServletResponse>()

    val request: HttpServletRequest
        get() = requestHolder.get()
    val response: HttpServletResponse
        get() = responseHolder.get()

    override fun init(filterConfig: FilterConfig) {
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        requestHolder.set(request as BufferingRequestWrapper)
        responseHolder.set(response as HttpServletResponse)
        try {
            chain.doFilter(request, response)
        } finally {
            requestHolder.remove()
            responseHolder.remove()
        }
    }

    override fun destroy() {
    }




}
