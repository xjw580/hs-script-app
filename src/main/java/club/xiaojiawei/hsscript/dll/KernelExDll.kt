package club.xiaojiawei.hsscript.dll

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.WinNT


/**
 * @author 肖嘉威
 * @date 2025/3/7 16:55
 */
@Suppress("ktlint:standard:function-naming")
interface KernelExDll : Library {

    fun OpenFileMappingW(dwDesiredAccess: Int, bInheritHandle: Boolean, lpName: WString?): WinNT.HANDLE?

    fun OpenEventW(dwDesiredAccess: Int, bInheritHandle: Boolean, lpName: WString?): WinNT.HANDLE?

    fun MapViewOfFile(
        hFileMappingObject: Pointer?, dwDesiredAccess: Int,
        dwFileOffsetHigh: Int, dwFileOffsetLow: Int, dwNumberOfBytesToMap: Int
    ): Pointer?

    fun UnmapViewOfFile(lpBaseAddress: Pointer?): Boolean

    fun CloseHandle(hObject: Pointer?): Boolean

    companion object {
        val INSTANCE: KernelExDll by lazy {
            Native.load("kernel32", KernelExDll::class.java)
        }

        const val FILE_MAP_READ: Int = 0x0004
    }
}
