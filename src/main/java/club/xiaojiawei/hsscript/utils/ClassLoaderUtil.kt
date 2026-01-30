package club.xiaojiawei.hsscript.utils

import java.io.File
import java.net.URL
import java.net.URLClassLoader


/**
 * @author 肖嘉威
 * @date 2024/9/6 22:08
 */
object ClassLoaderUtil {

    fun getClassLoader(path: File): Result<MutableList<ClassLoader?>> {
        val classLoaderList: MutableList<ClassLoader?> = ArrayList<ClassLoader?>()
        return if (path.exists()) {
            val files = path.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.name.endsWith(".jar")) {
                        classLoaderList.add(
                            URLClassLoader(
                                arrayOf<URL>(file.toURI().toURL()),
                                Thread.currentThread().getContextClassLoader()
                            )
                        )
                    }
                }
            }
            Result.success(classLoaderList)
        } else {
            Result.failure(Throwable("目录不存在"))
        }
    }

}
