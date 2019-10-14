binpack
=======
Binpack is a simple binary serialization library for Kotlin.
The goals are low output size and convenient API.
You can use it any time objects need to be persisted, for example save to file, key-value store, custom network protocol, etc.


### Installation
Using Gradle:
```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
...
dependencies {
    ...
    implementation 'com.github.And390:binpack:master-SNAPSHOT'
}
```

### Usage

The simplest example:
```kotlin
import and390.binpack.Serializer

class Person(
    val name: String,
    val age: Int?                
)

fun main() {
    val serializer = Serializer()
    val buffer = serializer.write(Person("Jon Snow", 35))
    val person = serializer.read<Person>(buffer)
}
```

Basically, you only need a `Serializer` which contain methods to read/write objects from/to ByteArray.
Note, that it is not thread safe, each thread should use its own instance.

Each serialized class should have a Kotlin primary constructor 
and for each constructor parameter, there must be a property with the same name (in this or parent class).
All other properties that are not in the constructor are ignored.

Binpack does not store any schema or type information, it must be known at compile time
 (the only exception is saving polymorphic objects).

All possible object graphs supported except loops (which leads to infinite recursion). Each reference is treated as a separate object. 
This means if an object appears in an object graph multiple times, it will be written multiple times and will be deserialized as different objects.

If a class (or interface) has subclasses then binpack saves its type when serializing base class
 and creates the correct subclass when deserializing. Thus, polymorphic objects are supported.
But the serializer must know all the used subclasses of the base class before serializing the base class
(because there is no easy way in JVM to get all possible subclasses of a particular class).
For this you should call `read`, `write` or `getSerializer` for each subclass before calling it for the base class.

```kotlin
interface Shape

class Circle(
    val radius: Float
) : Shape

class Rectangle(
    val width: Float,
    val height: Float
) : Shape

class Container(
    val shapes: List<Shape>
)

// specify all sublasses before using polymorphic objects serialization
serializer.getSerializer(Circle::class.java)
serializer.getSerializer(Rectangle::class.java)

val buffer1 = serializer.write(Circle(5f), Shape::class.java)  //it's important to specify base class here if you want polymorphic deserialization
val buffer2 = serializer.write(Rectangle(4f,3f), Shape::class.java)
val shape1 = serializer.read<Shape>(buffer1)  //returns Circle instance
val shape2 = serializer.read<Shape>(buffer2)  //returns Rectangle instance

val buffer3 = serializer.write(Container(listOf(Circle(5f),Rectangle(4f,3f))))
val container = serializer.read<Container>(buffer3)  //contains a list of different shapes
```
