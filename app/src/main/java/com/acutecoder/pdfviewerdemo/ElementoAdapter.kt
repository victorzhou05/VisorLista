package com.acutecoder.pdfviewerdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adaptador para nuestro RecyclerView de Elemento
 * Se encarga de inflar el layout de cada ítem y enlazar los datos
 *
 * elementos: lista de elementos a mostrar
 * listener: Callback para manejar el click en cada elemento
 */
class ElementoAdapter(
    var elementos: List<Elemento>,
    private val listener: OnElementoClickListener
) : RecyclerView.Adapter<ElementoAdapter.ViewHolder>() {

    /**
     * Interfaz que implementará quien quiera reaccionar al click
     * sobre un Elemento de la lista.
     */
    interface OnElementoClickListener {
        fun onElementoClick(elemento: Elemento)
    }

    /**
     * Se llama al crear cada ViewHolder:
     * Infla R.layout.item_elemento y lo envuelve en un ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_elemento, parent, false)
        return ViewHolder(view)
    }

    /**
     * Enlaza los datos de un Elemento con las vistas del ViewHolder
     * - nombre -> tvNombre
     * - descripción -> tvDescripcion
     * Además añade el listener para el click
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val elemento = elementos[position]

        holder.tvNombre.text = elemento.nombre ?: "Sin nombre"
        if(elemento.type == "curso") {
            holder.tvNombre.setTextColor(
                holder.itemView.context.getColor(R.color.white)
            )
            holder.itemView.setBackgroundColor(
                holder.itemView.context.getColor(R.color.colorSecondary)
            )
        } else if(elemento.type == "documento") {
            holder.tvNombre.setTextColor(
                holder.itemView.context.getColor(R.color.black)
            )
            holder.itemView.setBackgroundColor(
                holder.itemView.context.getColor(R.color.colorPrimary)
            )
        }

        holder.itemView.setOnClickListener {
            listener.onElementoClick(elemento)
        }
    }

    /**
     * Número de ítems que devuelve al RecyclerView.
     */
    override fun getItemCount(): Int = elementos.size

    /**
     * ViewHolder que guarda referencias a los TextView de cada ítem
     * para no buscarlos cada vez con findViewById
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
    }
}
