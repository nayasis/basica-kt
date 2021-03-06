package com.github.nayasis.kotlin.basica.core.resource.finder

import com.github.nayasis.kotlin.basica.core.resource.invocation.VfsVisitor
import com.github.nayasis.kotlin.basica.core.resource.matcher.PathMatcher
import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import com.github.nayasis.kotlin.basica.core.resource.util.VfsUtils
import java.io.IOException
import java.net.URL

class VfsResourceFinder(private var pathMatcher: PathMatcher) {

    @Throws(IOException::class)
    fun find(rootDir: URL?, pattern: String): Set<Resource> {
        val root = VfsUtils.getRoot(rootDir)
        val visitor = VfsVisitor(VfsUtils.getPath(root), pattern, pathMatcher)
        VfsUtils.visit(root, visitor)
        return visitor.getResources()
    }
}