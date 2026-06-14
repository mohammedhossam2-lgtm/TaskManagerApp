package com.taskmanager.app

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ThreadAdapter(
    private val messages: MutableList<ThreadMessage>,
    private val myId: Int
) : RecyclerView.Adapter<ThreadAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvAuthor  : TextView = v.findViewById(R.id.tvAuthor)
        val tvMessage : TextView = v.findViewById(R.id.tvMessage)
        val tvTime    : TextView = v.findViewById(R.id.tvTime)
        val tvFiles   : TextView = v.findViewById(R.id.tvFiles)
    }

    override fun getItemViewType(pos: Int): Int =
        if (messages[pos].authorId == myId) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = if (viewType == 1) R.layout.item_message_mine else R.layout.item_message_other
        val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val msg = messages[pos]
        h.tvAuthor.text  = msg.authorName
        h.tvMessage.text = msg.message.ifEmpty { "(مرفق)" }
        h.tvTime.text    = formatTime(msg.timestamp)
        val filesText = msg.attachments.joinToString("\n") { "📎 ${it.originalName}" }
        h.tvFiles.text = filesText
        h.tvFiles.visibility = if (filesText.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun getItemCount() = messages.size

    fun addMessages(newMessages: List<ThreadMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    private fun formatTime(iso: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            val date = sdf.parse(iso.take(19)) ?: return iso
            val out = java.text.SimpleDateFormat("d MMM، h:mm a", java.util.Locale("ar"))
            out.format(date)
        } catch (e: Exception) { iso }
    }
}
