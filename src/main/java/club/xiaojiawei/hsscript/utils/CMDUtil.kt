package club.xiaojiawei.hsscript.utils

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * @author 肖嘉威
 * @date 2024/9/7 23:49
 */
object CMDUtil {

    data class CommandResult(
        val output: String,
        val exitCode: Int
    )

    fun exec(command: String, outputCallback: (String) -> Unit = {}): CommandResult {
        val sb = StringBuilder()
        val process = Runtime.getRuntime().exec(command)
//        val process = ProcessBuilder(*command).start()

        // 读取标准输出
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                outputCallback(line!!)
                sb.append(line).append("\n")
            }
        }

        // 等待进程结束并获取退出码
        val exitCode = process.waitFor()

        return CommandResult(sb.toString(), exitCode)
    }

    fun exec(command: Array<String>, outputCallback: (String) -> Unit = {}): CommandResult {
        val sb = StringBuilder()
        val process = Runtime.getRuntime().exec(command)
//        val process = ProcessBuilder(*command).start()

        // 读取标准输出
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                outputCallback(line!!)
                sb.append(line).append("\n")
            }
        }

        // 等待进程结束并获取退出码
        val exitCode = process.waitFor()

        return CommandResult(sb.toString(), exitCode)
    }

    fun directExec(command: Array<String>): Process = Runtime.getRuntime().exec(command)

}
