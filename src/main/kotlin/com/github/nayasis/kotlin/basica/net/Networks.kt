package com.github.nayasis.kotlin.basica.net

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class Networks { companion object {

    /**
     * ignore SSL certification validation
     */
    fun trustAllCerts() {
        val allCerts = arrayOf(
            object: X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return emptyArray()
                }
            }
        )
        try {
            SSLContext.getInstance("SSL").apply {
                init(null, allCerts, SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory)
            }
        } catch (e: GeneralSecurityException) {}
    }

}}