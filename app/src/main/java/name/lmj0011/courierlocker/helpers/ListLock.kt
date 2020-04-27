package name.lmj0011.courierlocker.helpers

object ListLock {
    var isListLocked = false
        private set

    fun lock() {
        isListLocked = true
    }

    fun unlock() {
        isListLocked = false
    }
}