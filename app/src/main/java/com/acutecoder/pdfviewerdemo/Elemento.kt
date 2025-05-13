package com.acutecoder.pdfviewerdemo

data class Elemento(
    var type: String? = null,
    var nombre: String? = null,
    var descripcion: String? = null,
    var subelementos: List<Elemento>? = null
)