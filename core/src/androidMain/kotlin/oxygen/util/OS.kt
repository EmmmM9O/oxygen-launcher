package oxygen.util

import oxygen.*

actual fun OSFilesDir(): Fi = Fi(AndroidCore.context.getExternalFilesDir(null)!!.getAbsolutePath())

actual fun OSCacheDir(): Fi = Fi(AndroidCore.context.cacheDir)

actual fun OSRuntimeDir(): Fi = Fi(AndroidCore.context.getDir("runtime", 0)!!.getAbsolutePath())

actual fun OSNativeLibDir(): Fi = Fi(AndroidCore.context.applicationInfo.nativeLibraryDir)
