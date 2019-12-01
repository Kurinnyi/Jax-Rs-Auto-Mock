package ua.kurinnyi.jaxrs.auto.mock.filters

import javax.servlet.*

class ResponseIntersectingFilter : Filter {

    companion object {
        private val taskList:ThreadLocal<List<(ServletRequest, ServletResponse) -> Unit>> =
                ThreadLocal.withInitial { emptyList<(ServletRequest, ServletResponse) -> Unit>() }

        fun addTask(task:(ServletRequest, ServletResponse) -> Unit) {
            taskList.set(taskList.get() + task)
        }
    }



    override fun init(filterConfig: FilterConfig?) {
    }

    override fun destroy() {
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            chain.doFilter(request, response)
            taskList.get().forEach{ it(request, response)}
        } finally {
            taskList.remove()
        }
    }




}