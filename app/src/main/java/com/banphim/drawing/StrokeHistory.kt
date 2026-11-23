package com.banphim.drawing

class StrokeHistory {
    private val undoStack = ArrayDeque<Stroke>()
    private val redoStack = ArrayDeque<Stroke>()

    fun push(stroke: Stroke) {
        undoStack.addLast(stroke)
        redoStack.clear()
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        redoStack.addLast(undoStack.removeLast())
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        undoStack.addLast(redoStack.removeLast())
        return true
    }

    fun clear(): Boolean {
        if (undoStack.isEmpty() && redoStack.isEmpty()) return false
        undoStack.clear()
        redoStack.clear()
        return true
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun strokes(): List<Stroke> = undoStack.toList()
}
