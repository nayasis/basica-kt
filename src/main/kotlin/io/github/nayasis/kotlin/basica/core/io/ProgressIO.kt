package io.github.nayasis.kotlin.basica.core.io

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolute

class ProgressIO { companion object {

    /**
     * Copy file
     *
     * @param source    source file
     * @param target    target file
     * @param overwrite if true and target exists, delete target
     * @param skippable if true and target exists, skip copy process.
     * @param callback  progress worker
     *   - read : copied buffer size
     *   - done : read done size
     *   - total : source's total size
     */
    fun copyFile(source: Path, target: Path, overwrite: Boolean = true, skippable: Boolean = false, callback: ((read: Long, done: Long, total: Long) -> Unit)? = null) {
        if(!source.isFile())
            throw FileNotFoundException("$source")
        if(target.isFile()) {
            if(overwrite) {
                target.delete()
            } else if(skippable) {
                source.fileSize.let { callback?.invoke(it,it,it) }
            } else {
                throw FileAlreadyExistsException(target.toFile())
            }
        }
        val desc = if(target.isDirectory()) target.resolve(source.fileName) else {
            target.parent.makeDir()
            target
        }
        FileInputStream(source.toFile()).channel.use { from ->
            val channel = CallbackReadableChannel(from) { read, total -> callback?.invoke(read,total,source.fileSize) }
            FileOutputStream(desc.toFile()).channel.use { to ->
                to.transferFrom(channel,0,Long.MAX_VALUE)
            }
        }
    }

    /**
     * Move file
     *
     * @param source    source file
     * @param target    target file
     * @param overwrite if true and target exists, delete target
     * @param skippable if true and target exists, skip copy process.
     * @param callback  progress worker
     *   - read : moved buffer size
     *   - done : read done size
     *   - total : source's total size
     */
    fun moveFile(source: Path, target: Path, overwrite: Boolean = true, skippable: Boolean = false, callback: ((read: Long, done: Long, total: Long) -> Unit)? = null) {
        if(!source.isFile())
            throw FileNotFoundException("$source")
        if(source == target)
            throw FileAlreadyExistsException(source.toFile(), reason = "Could not move same file.")
        if(source.absolute().root == target.absolute().root ) {
            val option = toCopyOptions(overwrite)
            val fileSize = source.fileSize
            Files.move(source,target,*option)
            callback?.invoke(fileSize,fileSize,fileSize)
        } else {
            copyFile(source,target,overwrite,skippable,callback)
            val desc = if(target.isDirectory()) target.resolve(source.fileName) else target
            source.copyAttribute(desc)
            source.delete()
        }
    }

    /**
     * Copy directory
     *
     * @param source    source directory
     * @param target    target directory
     * @param overwrite if true and target has same file in source's, delete it
     * @param skippable if true and target has same file in source's, skip copy process.
     * @param callback  progress worker
     *   - index : copied file index
     *   - file  : current working file
     *   - read  : copied buffer size
     *   - done  : read done size
     */
    fun copyDirectory(source: Path, target: Path, overwrite: Boolean = true, skippable: Boolean = false, callback:((index: Int, file: Path, read: Long, done: Long) -> Unit)?) {
        if(!source.isDirectory())
            throw FileNotFoundException("No directory found (${source})")
        if(target.isFile())
            throw FileAlreadyExistsException(target.toFile(), reason = "Target is not directory")
        if(target.notExists())
            target.makeDir()
        var index = 0
        Files.walkFileTree(source, object: SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                target.resolve(source.relativize(dir)).let {
                    if( it != target ) {
                        it.makeDir()
                    }
                }
                return FileVisitResult.CONTINUE
            }
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                index++
                copyFile(file,target.resolve(source.relativize(file)),overwrite,skippable) { read, done, _ ->
                    callback?.invoke(index,file,read,done)
                }
                return FileVisitResult.CONTINUE
            }
        })
    }

    /**
     * Move directory
     *
     * @param source    source directory
     * @param target    target directory
     * @param overwrite if true and target has same file in source's, delete it
     * @param skippable if true and target has same file in source's, skip copy process.
     * @param callback  progress worker
     *   - index : moved file index
     *   - file  : current working file
     *   - read  : moved buffer size
     *   - done  : read done size
     */
    fun moveDirectory(source: Path, target: Path, overwrite: Boolean = true, skippable: Boolean = false, callback:((index: Int, file: Path, read: Long, done: Long) -> Unit)?) {
        if(!source.isDirectory())
            throw FileNotFoundException("No directory found (${source})")
        if(target.isFile())
            throw FileAlreadyExistsException(target.toFile(), reason = "Target is not directory")
        if(target.notExists())
            target.makeDir()
        var index = 0
        Files.walkFileTree(source, object: SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                target.resolve(source.relativize(dir)).let {
                    if( it != target ) {
                        it.makeDir()
                        dir.copyAttribute(it)
                    }
                }
                return FileVisitResult.CONTINUE
            }
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                index++
                moveFile(file,target.resolve(source.relativize(file)),overwrite,skippable){ read, done, _ ->
                    callback?.invoke(index,file,read,done)
                }
                return FileVisitResult.CONTINUE
            }
        })
        source.delete()
    }

    private fun toCopyOptions(overwrite: Boolean) =
        if (overwrite) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()

}}

class CallbackReadableChannel(
    private val rbc: ReadableByteChannel,
    private val callback: (read: Long, total: Long) -> Unit,
): ReadableByteChannel {

    private var total = 0L

    override fun close() = rbc.close()

    override fun isOpen(): Boolean = rbc.isOpen

    override fun read(buffer: ByteBuffer): Int {
        return rbc.read(buffer).also {
            if( it > 0 ) {
                total += it
                callback.invoke(it.toLong(), total)
            }
        }
    }

}