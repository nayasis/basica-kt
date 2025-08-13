package io.github.nayasis.kotlin.basica.core.resource.finder

import io.github.nayasis.kotlin.basica.core.resource.invocation.VfsVisitor
import io.github.nayasis.kotlin.basica.core.resource.matcher.PathMatcher
import io.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import io.github.nayasis.kotlin.basica.core.resource.util.VfsUtils
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