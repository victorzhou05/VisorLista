package com.acutecoder.visoreducativo

/**
 * Tipo de elemento en la estructura de navegación
 * Puede ser de tipo "curso" (contiene subelementos) o "documento" (enlace a nuestro PDF)
 *
 * type: Tipo de elemento: "curso" o "documento"
 * nombre: Texto que se mostrará como título en la lista
 * descripcion: explicación del propio elemento
 * subelementos: lista de los elementos contenidos (si type es "curso")
 */
data class Elemento(
    var type: String? = null,
    var nombre: String? = null,
    var descripcion: String? = null,
    var subelementos: List<Elemento>? = null
)
