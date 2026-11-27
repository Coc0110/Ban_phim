package com.banphim.drawing

class StrokeHistory {
    private sealed class HistoryAction {
        data class AddStroke(val stroke: Stroke) : HistoryAction()
        data class ClearAll(val clearedStrokes: List<Stroke>) : HistoryAction()
    }

    private val currentStrokes = mutableListOf<Stroke>()
    private val undoActions = ArrayDeque<HistoryAction>()
    private val redoActions = ArrayDeque<HistoryAction>()

    fun push(stroke: Stroke) {
        currentStrokes.add(stroke)
        undoActions.addLast(HistoryAction.AddStroke(stroke))
        redoActions.clear()
    }

    fun undo(): Boolean {
        if (undoActions.isEmpty()) return false
        val action = undoActions.removeLast()
        when (action) {
            is HistoryAction.AddStroke -> currentStrokes.removeAt(currentStrokes.lastIndex)
            is HistoryAction.ClearAll -> currentStrokes.addAll(action.clearedStrokes)
        }
        redoActions.addLast(action)
        return true
    }

    fun redo(): Boolean {
        if (redoActions.isEmpty()) return false
        val action = redoActions.removeLast()
        when (action) {
            is HistoryAction.AddStroke -> currentStrokes.add(action.stroke)
            is HistoryAction.ClearAll -> currentStrokes.clear()
        }
        undoActions.addLast(action)
        return true
    }

    fun clear(): Boolean {
        if (currentStrokes.isEmpty()) return false
        val snapshot = currentStrokes.toList()
        currentStrokes.clear()
        undoActions.addLast(HistoryAction.ClearAll(snapshot))
        redoActions.clear()
        return true
    }

    fun canUndo(): Boolean = undoActions.isNotEmpty()

    fun canRedo(): Boolean = redoActions.isNotEmpty()

    fun strokes(): List<Stroke> = currentStrokes.toList()
}
