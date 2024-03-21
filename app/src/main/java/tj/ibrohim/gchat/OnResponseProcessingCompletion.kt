package tj.ibrohim.gchat

interface OnResponseProcessingCompletion {
    fun blockInput()
    fun unblockInput()
    fun showError(error: Exception)
}