package minerva.android.walletmanager.utils

fun isNewVersionBigger(oldVersion: String?, newVersion: String): Boolean {
    if (oldVersion == null) return true

    val oldVersionList = oldVersion.split(SEPARATOR).map { it.toInt() }
    val newVersionList = newVersion.split(SEPARATOR).map { it.toInt() }

    newVersionList.forEachIndexed { index, value ->
        if (value > oldVersionList[index]) {
            return true
        } else if (value < oldVersionList[index]) {
            return false
        }
    }
    return false
}

private const val SEPARATOR = "."