package com.github.nayasis.kotlin.basica.core.string.binder

class BindingKey {

    var name: String = ""
    var format: String = ""

    constructor( info: String?, index: Int ) {

        if( ! info.isNullOrEmpty() ) {
            with(info.split(":")) {
                name = this.first()
                if( this.size >= 2 ) {
                    format = this[2]
                }
            }

            if( name.isEmpty() ) {
                name = FORMAT_INDEX.format(index)
            }

        }

    }

}