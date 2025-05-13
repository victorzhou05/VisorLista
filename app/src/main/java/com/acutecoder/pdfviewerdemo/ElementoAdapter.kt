package com.acutecoder.pdfviewerdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ElementoAdapter(
    var elementos: List<Elemento>,
    private val listener: OnElementoClickListener
) : RecyclerView.Adapter<ElementoAdapter.ViewHolder>() {

    interface OnElementoClickListener {
        fun onElementoClick(elemento: Elemento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_elemento, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val elemento = elementos[position] ?: return

        holder.tvNombre.text = elemento.nombre ?: "Sin nombre"
        holder.tvDescripcion.text = elemento.descripcion ?: "Sin descripción"

        holder.itemView.setOnClickListener {
            listener.onElementoClick(elemento)
        }
    }

    override fun getItemCount(): Int = elementos.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
    }
}