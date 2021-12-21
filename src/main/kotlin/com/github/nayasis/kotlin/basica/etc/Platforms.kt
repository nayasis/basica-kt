@file:JvmName("Platforms")

package com.github.nayasis.kotlin.basica.etc

class Platforms { companion object {

    val os = Os(
        name = System.getProperty("os.name").lowercase(),
        architecture = System.getProperty("os.arch"),
        version = System.getProperty("os.version"),
        charset = System.getProperty("sun.jnu.encoding"),
    )

    /** Java Virtual Machine architect  */
    val jvm = System.getProperty("sun.arch.data.model",System.getProperty("com.ibm.vm.bitmode"))

    /** is WINDOWS O/S  */
    val isWindows = os.name.contains("win")

    /** is LINUX O/S  */
    val isLinux = os.name.contains("linux")

    /** is UNIX O/S  */
    val isUnix = os.name.contains("unix")

    /** is SOLARIS O/S  */
    val isSolaris = os.name.contains("solaris") || os.name.contains("sunos")

    /** is MAC O/S  */
    val isMac = os.name.contains("mac")

    /** is MAC O/S  */
    val isAndroid = System.getProperty("java.vm.name") == "Dalvik"

}}

data class Os(
    val name: String,
    val architecture: String,
    val version: String,
    val charset: String,
)