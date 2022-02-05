package liklibs.db.delegates

import liklibs.db.*
import liklibs.db.annotations.DBTable
import liklibs.db.annotations.Primary
import liklibs.db.utlis.DBUtils
import org.intellij.lang.annotations.Language
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.hasAnnotation

object DBProperty {
    open class Property<V>(var value: V) : ReadWriteProperty<Any?, V> {

        override fun getValue(thisRef: Any?, property: KProperty<*>): V = value

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
            this.value = value

            if (!property.hasAnnotation<Primary>()) changeValueInDB(thisRef, property, value)
        }

        private fun changeValueInDB(thisRef: Any?, property: KProperty<*>, value: V) {
            val id = thisRef?.getPropertyWithAnnotation<Primary>()
                ?: throw IllegalStateException("No primary property in dependency class")
            val tableName =
                thisRef.annotation<DBTable>()?.tableName ?: throw IllegalStateException("Provide table name")

            val thisList = lists[thisRef::class.simpleName] ?: throw IllegalStateException("No list created")
            thisList.save()

            if (!thisList.utils.isAvailable) return

            @Language("PostgreSQL")
            val query = "UPDATE $tableName SET ${property.dbFieldName()} = ${DBUtils.parseValue(value)} WHERE _id = $id"
            thisList.utils.execute(query)
        }
    }

    fun <T> dbProperty(i: T): Property<T> = Property(i)
}


