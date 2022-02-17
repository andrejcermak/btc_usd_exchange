package com.example.demo.auth

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebArgumentResolver.UNRESOLVED
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.util.WebUtils
import javax.servlet.http.HttpServletRequest

class AuthTokenWebResolver : HandlerMethodArgumentResolver {

    @Autowired
    lateinit var authTokenHandler: AuthTokenHandler

    // to register com.example.demo.utils.Auth annotation
    override fun supportsParameter(methodParameter: MethodParameter): Boolean {
        return methodParameter.getParameterAnnotation(Auth::class.java) != null
    }

    override fun resolveArgument(parameter: MethodParameter,
                                 mavContainer: ModelAndViewContainer?,
                                 webRequest: NativeWebRequest,
                                 binderFactory: WebDataBinderFactory?): Any? {
        if (parameter.parameterType == Long::class.java) {
            // looking for the auth token in the headers
            var authToken = webRequest.getHeader("authToken")
            // looking for the auth token in the cookies
            if (authToken == null) {
                val servletRequest = webRequest.nativeRequest as HttpServletRequest

                val authTokenCookie = WebUtils.getCookie(servletRequest, "authToken")

                if (authTokenCookie != null)
                    authToken = authTokenCookie.value
            }
            val userId =  authTokenHandler.getUserFromToken(authToken)
            return userId?: throw AuthException()
        }

        return UNRESOLVED
    }
}