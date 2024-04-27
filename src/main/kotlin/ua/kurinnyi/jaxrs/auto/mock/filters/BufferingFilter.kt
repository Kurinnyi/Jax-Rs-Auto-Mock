package ua.kurinnyi.jaxrs.auto.mock.filters

import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class BufferingFilter : Filter {
    override fun destroy() {
    }

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain) {
        chain.doFilter(BufferingRequestWrapper(request as HttpServletRequest), BufferingResponseWrapper(response as HttpServletResponse))
    }

    override fun init(filterConfig: FilterConfig?) {
    }
}