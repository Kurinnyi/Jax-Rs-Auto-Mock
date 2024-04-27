package ua.kurinnyi.jaxrs.auto.mock.mocks

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

class ParametrizedTypeImpl (
    private var actualTypeArguments: Array<Type>,
    private var rawType: Class<*>,
    private var ownerType: Type? = rawType.declaringClass as Type?
): ParameterizedType {

    override fun getActualTypeArguments(): Array<Type> {
        return actualTypeArguments.clone()
    }
    override fun getRawType(): Class<*> {
        return this.rawType
    }

    override fun getOwnerType(): Type? {
        return this.ownerType
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ParameterizedType) {
            return false
        }
        if (this === other) {
            return true
        }
        return this.ownerType == other.ownerType
                && (this.rawType == other.rawType)
                && actualTypeArguments.contentEquals(other.actualTypeArguments)
    }

    override fun hashCode() = Objects.hash(*actualTypeArguments, this.ownerType, this.rawType)
}