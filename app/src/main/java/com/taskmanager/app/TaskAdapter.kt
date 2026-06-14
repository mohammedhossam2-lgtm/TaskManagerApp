package com.taskmanager.app

import android.graphics.Color
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: List<Task>,
    private var directory: List<User>,
    private val onClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle    : TextView = v.findViewById(R.id.tvTitle)
        val tvMeta     : TextView = v.findViewById(R.id.tvMeta)
        val tvStatus   : TextView = v.findViewById(R.id.tvStatus)
        val tvPriority : TextView = v.findViewById(R.id.tvPriority)
        val unreadDot  : View     = v.findViewById(R.id.unreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val task = tasks[pos]
        val person = directory.find { it.id == task.assignedTo }

        h.tvTitle.text = task.title
        h.tvMeta.text  = buildString {
            append("👤 ${person?.name ?: "—"}")
            if (task.dueDate.isNotEmpty()) append("  📅 ${task.dueDate}")
            if (task.overdue) append("  ⚠️ متأخر")
        }
        h.tvStatus.text = when(task.status) {
            "pending"  -> "⏳ معلقة"
            "progress" -> "🔄 جارية"
            "done"     -> "✅ منجزة"
            else -> task.status
        }
        h.tvPriority.text = when(task.priority) {
            "high" -> "🔴"
            "low"  -> "🟢"
            else   -> "🟡"
        }

        h.unreadDot.visibility = if (task.unread) View.VISIBLE else View.GONE

        // Card left-border color by status
        val borderColor = when(task.status) {
            "pending"  -> Color.parseColor("#f4b942")
            "progress" -> Color.parseColor("#4f8ef7")
            "done"     -> Color.parseColor("#3ecf8e")
            else -> Color.GRAY
        }
        h.itemView.setBackgroundColor(Color.TRANSPARENT)

        h.itemView.setOnClickListener { onClick(task) }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    fun updateDirectory(newDir: List<User>) {
        directory = newDir
        notifyDataSetChanged()
    }
}
